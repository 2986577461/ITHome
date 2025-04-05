package com.xiaoyan.service.impl;

import com.xiaoyan.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    @Value("${deepseek.base-url}")
    private String DEEPSEEK_API_URL;

    @Value("${deepseek.api-key}")
    private  String API_KEY; // 替换为你的 API Key

    public String send(String userMessage) {
        System.out.println(userMessage);
        RestTemplate restTemplate = new RestTemplate();

        // 构造请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);

        // 构造请求体（类似 OpenAI 格式）
        String requestBody = """
            {
                "model": "deepseek-chat",
                "messages": [
                    {"role": "user", "content": "%s"}
                ]
            }
            """.formatted(userMessage);

        // 发送 POST 请求
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                DEEPSEEK_API_URL,
                request,
                String.class
        );

        return response.getBody();
    }
}