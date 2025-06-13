package com.xiaoyan.controller;


import com.xiaoyan.context.BaseContext;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ChatService;
import com.xiaoyan.vo.StudentDialogVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("chat")
@Slf4j
@AllArgsConstructor
@Tag(name = "聊天管理")
public class ChatController {

    private ChatService chatService;

    @GetMapping
    public Result<List<StudentDialogVO>> getList() {
        log.info("获取用户列表");
        List<StudentDialogVO> list = chatService.getList();

        return Result.success(list);
    }

}
