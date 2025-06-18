package com.xiaoyan.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    @Value("${deepseek.base-url}")
    private String DEEPSEEK_API_URL;

    @Value("${deepseek.api-key}")
    private String API_KEY;

    // 使用Jackson ObjectMapper处理JSON
    private static final ObjectMapper mapper = new ObjectMapper();

    public String send(String userMessage) {
        RestTemplate restTemplate = new RestTemplate();
        // 1. 构造请求体对象
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "user",
                "content", userMessage
        ));
        requestBody.put("messages", messages);

        try {
            // 2. 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + API_KEY);

            // 3. 将请求体转为JSON字符串
            String requestBodyJson = mapper.writeValueAsString(requestBody);

            // 4. 发送请求
            HttpEntity<String> request = new HttpEntity<>(requestBodyJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    DEEPSEEK_API_URL,
                    request,
                    String.class
            );

            return response.getBody();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON处理失败", e);
        }
    }
}