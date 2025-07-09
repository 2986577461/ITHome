package com.xiaoyan.service.impl;

import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.service.ChatService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;



@Service
@AllArgsConstructor
public class ChatServiceImpl implements ChatService {

    private UserMapper userMapper;


}


