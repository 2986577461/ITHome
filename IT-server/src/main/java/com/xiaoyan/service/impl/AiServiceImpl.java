package com.xiaoyan.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.AiDialogSessionMapper;
import com.xiaoyan.mapper.AiDialogMapper;
import com.xiaoyan.pojo.AiDialogSession;
import com.xiaoyan.pojo.AiDialog;
import com.xiaoyan.service.AiService;
import com.xiaoyan.service.AiSessionService;
import jakarta.annotation.Resource;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
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

    @Resource
    private AiSessionService aiSessionService;

    @Resource
    private AiDialogSessionMapper aiDialogSessionMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    //                                  用户id    会话id     上下文  消息        属性
    private static final ConcurrentMap<Integer, ConcurrentMap<Long, List<Map<String, String>>>>
            MESSAGES = new ConcurrentHashMap<>();

    private static final Map<String, String> SYSTEM_PERSONA_MESSAGE =
            Map.of("role", "system", "content", """
                    你的人设:IT之家社团网站的助手
                    回答格式:纯文本,内容控制在3000个字符内""");

    public AiServiceImpl(WebClient.Builder webClientBuilder,
                         @Value("${deepseek.base-url}") String deepseekApiUrl,
                         @Value("${deepseek.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(deepseekApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Flux<String> streamChatCompletion(MessageDTO messageDTO, @NotNull Integer studentId) {
        Long sessionId = messageDTO.getSessionId();
        if (sessionId == null) {
            sessionId = aiSessionService.createSession(studentId);
        } else {
            AiDialogSession session = aiDialogSessionMapper.selectById(sessionId);
            if (session == null) {
                throw new ParameterException(MessageConstant.SESSION_NO_FOUND);
            }
        }
        Long sessionId1 = sessionId;

        //兼容旧id之家，取消身份判断
//        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
//        if (group == null || !group.getStudentId().equals(studentId))
//            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        //插入问题
        String message = messageDTO.getMessage();
        LocalDateTime now = LocalDateTime.now();
        aiDialogMapper.insert(AiDialog.builder().
                sessionId(sessionId).
                senderType("user").
                createDateTime(now).
                content(message).build());
        //更新会话最后活跃时间
        aiDialogSessionMapper.updateLastActiveDateTime(sessionId, now);

        ConcurrentMap<Long, List<Map<String, String>>> sessionMap = MESSAGES.
                computeIfAbsent(studentId, k -> new ConcurrentHashMap<>());
        List<Map<String, String>> conversation = sessionMap.
                computeIfAbsent(sessionId, integer -> new LinkedList<>());
        if (conversation.isEmpty()) {
            conversation.add(SYSTEM_PERSONA_MESSAGE);
        }

        // 确保不移除 SYSTEM_PERSONA_MESSAGE
        while (conversation.size() > contextLength && conversation.size() > 1) {
            // 总是移除第一个非系统消息
            conversation.remove(1);
        }

        conversation.add(Map.of("role", "user", "content", message));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", conversation);
        // 启用流式响应
        requestBody.put("stream", true);

        StringBuilder answer = new StringBuilder();
        String title = message.length() > 20 ? message.substring(0, 20) : message;
        try {
            return webClient.post()
                    .bodyValue(requestBody)
                    // 接受 SSE 流
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(fluxString -> {
                        try {
                            if ("[DONE]".equals(fluxString)) {
                                return;
                            }
                            JsonNode jsonNode = OBJECT_MAPPER.readTree(fluxString);
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

    @Override
    public void deleteSession(Long sessionId, Integer studentId) {
        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
        ConcurrentMap<Long, List<Map<String, String>>> sessionMap = MESSAGES.get(studentId);
        if (group == null|| sessionMap ==null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        if (!group.getStudentId().equals(studentId)) {
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);
        }

        aiSessionService.deleteSession(sessionId, studentId);
        sessionMap.remove(sessionId);
    }


    public void saveAnswer(String message, String title, Long sessionId, Integer studentId) {

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
        ConcurrentMap<Long, List<Map<String, String>>> sessionMap = MESSAGES.get(studentId);
        List<Map<String, String>> list = sessionMap.computeIfAbsent(sessionId, integer -> new LinkedList<>());
        list.add(Map.of("role", "assistant", "content", message));
    }
}