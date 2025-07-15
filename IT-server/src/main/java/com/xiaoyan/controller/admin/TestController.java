package com.xiaoyan.controller.admin;


import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("test")
@AllArgsConstructor
public class TestController {

    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public void getString() {
        List<String> testList = Arrays.asList("item1", "item2");
        redisTemplate.opsForValue().set("test:json:list", testList);
        System.out.println("Test data written with JSON serializer.");
    }
}
