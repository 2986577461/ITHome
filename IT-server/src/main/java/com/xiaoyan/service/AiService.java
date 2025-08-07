package com.xiaoyan.service;

import com.xiaoyan.dto.MessageDTO;

import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;


public interface AiService {
    Flux<String> streamChatCompletion(MessageDTO messageDTO,@NotNull Integer studentId);

    void deleteSession(Long sessionId,Integer studentId);
}
