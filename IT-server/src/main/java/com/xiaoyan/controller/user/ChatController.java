package com.xiaoyan.controller.user;


import com.xiaoyan.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("user/chat")
@AllArgsConstructor
@Tag(name = "聊天管理")
public class ChatController {

    private ChatService chatService;

}
