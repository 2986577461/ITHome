package com.xiaoyan.controller;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.dto.EmailDTO;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.EmailService;
import com.xiaoyan.service.impl.EmailServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/email")
@AllArgsConstructor
@Slf4j
@Tag(name = "电子邮件/可以绑定账号")
@Validated
public class EmailController {

    private JwtProperties jwtProperties;

    private EmailService emailService;

    // 发送验证码
    @PostMapping("/send-code")
    @Operation(summary = "发送邮件")
    public Result<String> sendCode(@NotBlank String email) {
        emailService.sendVerificationCode(email);
        return Result.success("验证码已发送");
    }

    // 校验验证码
    @PostMapping("/verify")
    @Operation(summary = "校验验证码")
    public Result<Boolean> verifyCode(@RequestBody @Valid EmailDTO emailDTO) {
        boolean isValid = emailService.verifyCode(emailDTO);
        if (!isValid)
            return Result.error(MessageConstant.VERIFICATION_CODE_MISMATCH);

        EmailServiceImpl.codePool.remove(emailDTO.getEmail());
//  todo
//        //登录成功后，生成jwt令牌
//        Map<String, Object> claims = new HashMap<>();
//        claims.put(jwtProperties.getTokenName(), emailDTO.getEmail());
//        String token = JwtUtil.createJWT(
//                jwtProperties.getSecretKey(),
//                jwtProperties.getTtl(),
//                claims);


        return Result.success();
    }
}