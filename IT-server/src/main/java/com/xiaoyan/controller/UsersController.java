package com.xiaoyan.controller;

import com.xiaoyan.annotation.CheckPrimaryKeyRepeat;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.StudentDTO;
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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("users")
@Slf4j
@AllArgsConstructor
@Tag(name = "用户管理")
public class UsersController {

    private UsersService userService;

    private JwtProperties jwtProperties;

    @GetMapping
    @Operation(summary = "返回当前学生信息")
    public Result<StudentVO> getUser() {
        StudentVO user = userService.getUser();
        return Result.success(user);
    }

    @PostMapping("login")
    @Operation(summary = "账号密码登录")
    public Result<StudentVO> login(@RequestBody LoginDTO message) {

        log.info("请求登陆：{}", message);
        StudentVO studentVO = userService.login(message);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(jwtProperties.getTokenName(), studentVO.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);
        studentVO.setToken(token);

        return Result.success(studentVO);
    }

    @PostMapping
    @Operation(summary = "返回所有学生信息")
    public Result<List<StudentVO>> getAll() {
        List<StudentVO> list = userService.getAll();
        return Result.success(list);
    }

    @DeleteMapping
    @Operation(summary = "删除学生")
    public Result<String> removeStudents(List<Long> ids) {
        userService.removeStudents(ids);
        return Result.success();
    }

    @PutMapping
    @CheckPrimaryKeyRepeat
    @Operation(summary = "修改学生信息包括密码")
    public Result<String> updateStudent(@RequestBody @Valid StudentDTO student) {
        userService.update(student);
        return Result.success();
    }
}
