package com.xiaoyan.service;

import com.xiaoyan.dto.MessageDTO;
import reactor.core.publisher.Flux;


public interface AIService {
    Flux<String> streamChatCompletion(Integer userId, String userMessage);

    void saveAnswer(MessageDTO messageDTO);
}
