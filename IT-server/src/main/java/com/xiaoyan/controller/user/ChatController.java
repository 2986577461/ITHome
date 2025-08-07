package com.xiaoyan.controller.user;


import com.xiaoyan.result.Result;
import com.xiaoyan.service.ChatService;
import com.xiaoyan.vo.StudentDialogVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("user/chat")
@AllArgsConstructor
@Tag(name = "聊天管理")
public class ChatController {

    private ChatService chatService;

    @GetMapping("all")
    public Result<List<StudentDialogVO>> getAllUser() {
       List<StudentDialogVO>list= chatService.getAllUser();
       return Result.success(list);
    }

}
