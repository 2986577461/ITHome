package com.xiaoyan.controller.user;


import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.AiService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.vo.AiDialogGroupVO;
import com.xiaoyan.vo.AiDialogVO;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;


@RestController
@RequestMapping("/user/ai-dialog")
@AllArgsConstructor
@Tag(name = "AI管理")
@Slf4j
public class AIController {

    private AiService aiService;

    private JwtProperties jwtProperties;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送问题，如果不携带sessionpId则创建新会话")
    public Flux<String> streamAiResponse(String message, @Schema(description = "会话id") Integer sessionId, String token) {
        //临时token校验
        temporaryJwtParse(token);
        return aiService.streamChatCompletion(BaseContext.getCurrentStudentId(), sessionId, message)
                .map(chunk -> "data: " + chunk + "\n");
    }

    private void temporaryJwtParse(String token) {
        Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
        Object object = claims.get(JwtClaimsConstant.USER_ID);
        Object object1 = claims.get(JwtClaimsConstant.ADMIN_ID);
        int studentId;
        if (object != null)
            studentId = Integer.parseInt(object.toString());
        else
            studentId = Integer.parseInt(object1.toString());

        BaseContext.setCurrentStudentId(studentId);
    }

    @PostMapping("assistant-answer")
    @Operation(summary = "保存AI回答")
    public Result<String> saveAnswer(@RequestBody @Valid MessageDTO messageDTO) {
        aiService.saveAnswer(messageDTO);
        return Result.success();
    }

    @GetMapping("all")
    @Operation(summary = "获取当前用户左侧的所有会话")
    public Result<List<AiDialogGroupVO>> getAll() {
        List<AiDialogGroupVO> list = aiService.getAll();
        return Result.success(list);
    }

    @GetMapping
    @Operation(summary = "给定会话id返回所有历史记录")
    public Result<List<AiDialogVO>> getMessages(Integer sessionId) {
        List<AiDialogVO> messages = aiService.getMessages(sessionId);
        return Result.success(messages);
    }

    @DeleteMapping("{sessionId}")
    @Operation(summary = "删除指定会话")
    public Result<String> deleteSession(@PathVariable Integer sessionId) {
        aiService.deleteSession(sessionId);
        return Result.success();
    }

}
