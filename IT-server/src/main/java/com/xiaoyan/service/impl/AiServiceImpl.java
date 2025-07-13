package com.xiaoyan.service.impl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.AiDialogSessionMapper;
import com.xiaoyan.mapper.AiDialogMapper;
import com.xiaoyan.pojo.AiDialogSession;
import com.xiaoyan.pojo.AiDialog;
import com.xiaoyan.service.AiService;
import com.xiaoyan.vo.AiDialogGroupVO;
import com.xiaoyan.vo.AiDialogVO;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
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

    @Resource
    private AiDialogSessionMapper aiDialogSessionMapper;

    private static final ConcurrentMap<Integer, List<Map<String, String>>> messages =
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

    public Flux<String> streamChatCompletion(Integer studentId, Integer sessionId, String message) {
        if (sessionId == null) {
            sessionId = this.createSession();
        }
        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
        if (group == null || !group.getStudentId().equals(studentId))
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        LocalDateTime now = LocalDateTime.now();
        aiDialogMapper.insert(AiDialog.builder().
                sessionId(sessionId).
                senderType("user").
                createDateTime(now).
                content(message).build());
        aiDialogSessionMapper.updateLastActiveDateTime(sessionId, now);

        List<Map<String, String>> conversation = messages.computeIfAbsent(studentId, k -> new ArrayList<>());
        if (conversation.isEmpty()) {
            conversation.add(SYSTEM_PERSONA_MESSAGE);
        }

        if (conversation.size() > 14) {
            conversation.removeFirst();
            conversation.removeFirst();
        }

        conversation.add(Map.of("role", "user", "content", message));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", conversation);
        requestBody.put("stream", true); // 启用流式响应

        try {
            return webClient.post()
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM) // 接受 SSE 流
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(chunk -> {
                    })
                    .doOnError(e -> log.error("Stream error", e));
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    private Integer createSession() {
        Integer currentStudentId = BaseContext.getCurrentStudentId();
        LocalDateTime now = LocalDateTime.now();

        return aiDialogSessionMapper.insert(AiDialogSession.
                builder().
                studentId(currentStudentId).
                createDateTime(now).
                LastActiveDateTime(now).build());
    }

    @Override
    public void saveAnswer(MessageDTO messageDTO) {
        Integer currentStudentId = BaseContext.getCurrentStudentId();
        String answer = messageDTO.getMessage();
        Integer sessionId = messageDTO.getSessionId();

        AiDialogSession session = aiDialogSessionMapper.selectById(sessionId);
        if ( !session.getStudentId().equals(currentStudentId))
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        if (session.getTitle() == null) {
            String title = answer.substring(0, 20);
            aiDialogSessionMapper.updateTitleByGroupId(sessionId, title);
        }


        aiDialogMapper.insert(AiDialog.
                builder().
                senderType("model").
                content(answer).
                sessionId(sessionId).
                createDateTime(LocalDateTime.now()).
                build()
        );
        List<Map<String, String>> list = messages.get(currentStudentId);
        list.add(Map.of("role", "assistant", "content", answer));
    }

    @Override
    public List<AiDialogGroupVO> getAll() {
        List<AiDialogSession> aiDialogSessions =
                aiDialogSessionMapper.selectByStudentId(BaseContext.getCurrentStudentId());
        List<AiDialogGroupVO> list = new ArrayList<>();
        for (AiDialogSession group : aiDialogSessions) {
            AiDialogGroupVO vo = new AiDialogGroupVO();
            BeanUtils.copyProperties(group, vo);
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<AiDialogVO> getMessages(Integer sessionId) {
        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
        if (!group.getStudentId().equals(BaseContext.getCurrentStudentId()))
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);
        List<AiDialog> list = aiDialogMapper.selectBySessionId(sessionId);
        List<AiDialogVO> voList = new ArrayList<>();
        for (AiDialog dialog : list) {
            AiDialogVO vo = new AiDialogVO();
            BeanUtils.copyProperties(dialog, vo);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public void deleteSession(Integer sessionId) {
        Integer currentStudentId = BaseContext.getCurrentStudentId();
        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
        if (group == null)
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);

        if (!group.getStudentId().equals(currentStudentId))
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        aiDialogSessionMapper.deleteById(sessionId);
        aiDialogMapper.deleteBySessionId(sessionId);
    }


}