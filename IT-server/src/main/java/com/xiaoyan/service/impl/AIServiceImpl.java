package com.xiaoyan.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.mapper.AIDialogMapper;
import com.xiaoyan.pojo.AiDialog;
import com.xiaoyan.service.AIService;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
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
public class AIServiceImpl implements AIService {

    private final WebClient webClient;

    @Resource
    private AIDialogMapper aiDialogMapper;

    private static final ConcurrentMap<Integer, List<Map<String, String>>> messages =
            new ConcurrentHashMap<>();

    private static final Map<String, String> SYSTEM_PERSONA_MESSAGE =
            Map.of("role", "system", "content", """
                    你的人设:IT之家社团网站的助手
                    回答风格:可爱型
                    回答格式:纯文本,内容控制在3000个字符内""");

    private static final ObjectMapper mapper = new ObjectMapper();

    public AIServiceImpl(WebClient.Builder webClientBuilder,
                         @Value("${deepseek.base-url}") String deepseekApiUrl,
                         @Value("${deepseek.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(deepseekApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Flux<String> streamChatCompletion(Integer userId, String userMessage) {
        List<Map<String, String>> conversation = messages.computeIfAbsent(userId, k -> new ArrayList<>());

        if (conversation.isEmpty()) {
            conversation.add(SYSTEM_PERSONA_MESSAGE);
        }

        if (conversation.size() > 14) {
            conversation.remove(0);
        }

        conversation.add(Map.of("role", "user", "content", userMessage));

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

    @Override
    public void saveAnswer(MessageDTO messageDTO) {
        String answer = messageDTO.getMessage();
        Integer senderId = BaseContext.getCurrentStudentId();
        List<Map<String, String>> list = messages.get(senderId);
        String content = list.getLast().get("content");

        aiDialogMapper.insert(AiDialog.
                builder().
                answer(answer).
                content(content).
                senderId(senderId).
                createDateTime(LocalDateTime.now()).
                build()
        );

        list.add(Map.of("role", "assistant", "content", answer));
    }
}