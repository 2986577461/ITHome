package com.xiaoyan.service;

import reactor.core.publisher.Flux;


public interface AIService {
    Flux<String> streamChatCompletion(Integer userId, String userMessage);
}
