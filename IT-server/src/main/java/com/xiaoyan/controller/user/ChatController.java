package com.xiaoyan.controller.user;


import com.xiaoyan.result.Result;
import com.xiaoyan.service.ChatService;
import com.xiaoyan.vo.StudentDialogVO;
import io.swagger.v3.oas.annotations.Operation;
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

    @GetMapping
    @Operation(summary = "返回用户列表")
    public Result<List<StudentDialogVO>> getList() {
        List<StudentDialogVO> list = chatService.getList();

        return Result.success(list);
    }

}
