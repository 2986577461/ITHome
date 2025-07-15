package com.xiaoyan.service;

import com.xiaoyan.dto.MessageDTO;

import reactor.core.publisher.Flux;


public interface AiService {
    Flux<String> streamChatCompletion(MessageDTO messageDTO,Integer studentId);
}
