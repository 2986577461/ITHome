package com.xiaoyan.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.AiDialogSessionMapper;
import com.xiaoyan.mapper.AiDialogMapper;
import com.xiaoyan.pojo.AiDialogSession;
import com.xiaoyan.pojo.AiDialog;
import com.xiaoyan.service.AiService;
import com.xiaoyan.service.AiSessionService;
import com.xiaoyan.vo.AiDialogSessionVO;
import com.xiaoyan.vo.AiDialogVO;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final WebClient webClient;

    @Resource
    private AiDialogMapper aiDialogMapper;

    @Value("${xiaoyan.context-length}")
    private Integer contextLength;

    @Autowired
    private AiSessionService aiSessionService;

    @Resource
    private AiDialogSessionMapper aiDialogSessionMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    //                                  用户id    会话id     上下文  消息        属性
    private static final ConcurrentMap<Integer, ConcurrentMap<Integer, List<Map<String, String>>>> messages =
            new ConcurrentHashMap<>();

    private static final Map<String, String> SYSTEM_PERSONA_MESSAGE =
            Map.of("role", "system", "content", """
                    你的人设:IT之家社团网站的助手
                    回答风格:可爱型
                    回答格式:纯文本,内容控制在3000个字符内""");

    public AiServiceImpl(WebClient.Builder webClientBuilder,
                         @Value("${deepseek.base-url}") String deepseekApiUrl,
                         @Value("${deepseek.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(deepseekApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Flux<String> streamChatCompletion(MessageDTO messageDTO, Integer studentId) {
        Integer sessionId = messageDTO.getSessionId();
        if (sessionId == null) {
            sessionId = aiSessionService.createSession(studentId);
        } else {
            AiDialogSession session = aiDialogSessionMapper.selectById(sessionId);
            if (session == null) {
                throw new ParameterException(MessageConstant.SESSION_NO_FOUND);
            }
        }
        Integer sessionId1 = sessionId;

        //兼容旧id之家，取消身份判断
//        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
//        if (group == null || !group.getStudentId().equals(studentId))
//            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        String message = messageDTO.getMessage();
        LocalDateTime now = LocalDateTime.now();
        aiDialogMapper.insert(AiDialog.builder().
                sessionId(sessionId).
                senderType("user").
                createDateTime(now).
                content(message).build());
        aiDialogSessionMapper.updateLastActiveDateTime(sessionId, now);

        ConcurrentMap<Integer, List<Map<String, String>>> sessionMap = messages.computeIfAbsent(studentId,
                k -> new ConcurrentHashMap<>());

        List<Map<String, String>> conversation = sessionMap.computeIfAbsent(sessionId, integer -> new ArrayList<>());
        if (conversation.isEmpty()) {
            conversation.add(SYSTEM_PERSONA_MESSAGE);
        }

        if (conversation.size() > contextLength) {
            conversation.remove(1);
            conversation.remove(1);
        }

        conversation.add(Map.of("role", "user", "content", message));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", conversation);
        requestBody.put("stream", true); // 启用流式响应

        StringBuilder answer = new StringBuilder();
        String title = message.length() > 20 ? message.substring(0, 20) : message;
        try {
            return webClient.post()
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM) // 接受 SSE 流
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(fluxString -> {
                        try {
                            if (fluxString.equals("[DONE]"))
                                return;
                            JsonNode jsonNode = objectMapper.readTree(fluxString);
                            String fluxSubString = jsonNode.path("choices")
                                    .get(0)
                                    .path("delta")
                                    .path("content")
                                    .asText();
                            answer.append(fluxSubString);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .doOnError(e -> log.error("Stream error", e))
                    .doOnComplete(() -> this.saveAnswer(answer.toString(), title, sessionId1, studentId));
        } catch (RuntimeException e) {
            return Flux.error(e);
        }
    }


    public void saveAnswer(String message, String title, Integer sessionId, Integer studentId) {

        AiDialogSession session = aiDialogSessionMapper.selectById(sessionId);
        //兼容旧it之家前端
//        if (!session.getStudentId().equals(studentId))
//         throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        if (session.getTitle() == null) {
            aiDialogSessionMapper.updateTitleByGroupId(sessionId, title);
        }

        aiDialogMapper.insert(AiDialog.
                builder().
                senderType("model").
                content(message).
                sessionId(sessionId).
                createDateTime(LocalDateTime.now()).
                build()
        );
        ConcurrentMap<Integer, List<Map<String, String>>> sessionMap = messages.get(studentId);
        List<Map<String, String>> list = sessionMap.computeIfAbsent(sessionId, integer -> new ArrayList<>());
        list.add(Map.of("role", "assistant", "content", message));
    }
}