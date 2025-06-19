package com.xiaoyan.controller.user;

import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;
import com.xiaoyan.dto.StudentDTO;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.vo.StudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController("userUser")
@RequestMapping("user/users")
@Slf4j
@AllArgsConstructor
@Tag(name = "自身信息维护以及登录操作")
public class UsersController {

    private UsersService userService;

    private JwtProperties jwtProperties;

    @PutMapping
    @Operation(summary = "修改密码")
    public Result<String> updateStudent(@RequestBody @Valid PasswordDTO passwordDTO) {
        userService.updatePassword(passwordDTO);
        return Result.success();
    }

    @PostMapping("login")
    @Operation(summary = "账号密码登录")
    public Result<StudentVO> login(@RequestBody LoginDTO message) {

        log.info("请求登陆：{}", message);
        StudentVO studentVO = userService.login(message);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(jwtProperties.getTokenName(), studentVO);
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);
        studentVO.setToken(token);

        return Result.success(studentVO);
    }
}
