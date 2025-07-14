package com.xiaoyan.controller.user;


import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.AiService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.vo.AiDialogSessionVO;
import com.xiaoyan.vo.AiDialogVO;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    @PostMapping(value = "send-message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送问题,")
    public Flux<String> streamAiResponse(@RequestBody @Valid MessageDTO messageDTO) {
        Integer studentId = BaseContext.getCurrentStudentId();
        log.info("{}从会话{}中询问AI:\"{}\"",studentId,messageDTO.getSessionId(),messageDTO.getMessage());
        return aiService.streamChatCompletion(messageDTO, studentId)
                .map(chunk -> "data: " + chunk + "\n");
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "旧版本，不要调用")
    @Deprecated(since="9", forRemoval=true)
    public Flux<String> streamAiResponse2(String message, @Schema(description = "会话id") Integer sessionId, String token) {
        //临时token校验
        temporaryJwtParse(token);

        Integer studentId = BaseContext.getCurrentStudentId();
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setSessionId(sessionId);
        messageDTO.setMessage(message);
        return aiService.streamChatCompletion(messageDTO, studentId)
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

    @GetMapping("all")
    @Operation(summary = "获取当前用户左侧的所有会话从最新到最旧")
    public Result<List<AiDialogSessionVO>> getAll() {
        Integer studentId = BaseContext.getCurrentStudentId();
        log.info("获取{}左侧的所有对话",studentId);
        List<AiDialogSessionVO> list = aiService.getAll(studentId);
        return Result.success(list);
    }

    @GetMapping("history")
    @Operation(summary = "给定会话id返回自己所有的历史记录")
    public Result<List<AiDialogVO>> getMessages(@NotNull Integer sessionId) {
        log.info("获取会话{}的AI对话记录",sessionId);
        List<AiDialogVO> messages = aiService.getMessages(sessionId);
        return Result.success(messages);
    }

    @DeleteMapping("{sessionId}")
    @Operation(summary = "删除指定会话")
    public Result<String> deleteSession(@PathVariable Integer sessionId) {
        log.info("删除会话:{}",sessionId);
        aiService.deleteSession(sessionId,BaseContext.getCurrentStudentId());
        return Result.success();
    }

}
