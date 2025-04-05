package com.xiaoyan.controller;


import com.xiaoyan.annotation.RequireLogin;
import com.xiaoyan.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("aiDialog")
public class DeepSeekController {

    @Autowired
    private DeepSeekService deepSeekService;

    @PostMapping
    @RequireLogin
    public String send(String userMessage) {
        return deepSeekService.send(userMessage);
    }

}
