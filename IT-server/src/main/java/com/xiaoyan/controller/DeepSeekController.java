package com.xiaoyan.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoyan.config.WebSocketServer;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.DeepSeekService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("ai-dialog")
@AllArgsConstructor
public class DeepSeekController {

    private DeepSeekService deepSeekService;

    @PostMapping
    public Result<String> send(@RequestBody String userMessage) throws JsonProcessingException {
        String message = deepSeekService.send(userMessage);
        JsonNode root = WebSocketServer.objectMapper.readTree(message);
        String content = root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
        return Result.success(content);
    }

}
