package com.xiaoyan.controller;

import com.xiaoyan.annotation.CheckPrimaryKeyRepeat;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.UpdateStudent;
import com.xiaoyan.pojo.ITStudent;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.vo.ITStudentVO;

import com.xiaoyan.vo.StudentGovernVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//等于ResponseBody+Controller
@RestController

//将请求方法的路径增加共同前缀users，
//这样请求该类的所有方法时路径为：localhost：8080/users/...
@RequestMapping("users")

//使该Controller接收跨域请求，也就是IP地址可以不同，
// 端口号相同即可
@Slf4j
@AllArgsConstructor
public class UsersController {

    private UsersService userService;

    private JwtProperties jwtProperties;

    @GetMapping
    public Result<ITStudentVO> getUser() {
        ITStudentVO user = userService.getUser();
        return Result.success(user);
    }

    @GetMapping("all")
    public Result<List<StudentGovernVO>> getAll() {
        List<StudentGovernVO> list = userService.getAll();
        return Result.success(list);
    }

    @PostMapping("login")
    public Result<ITStudentVO> login(@RequestBody LoginDTO message) {

        log.info("请求登陆：{}", message);
        ITStudentVO studentVO = userService.login(message);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(jwtProperties.getTokenName(), studentVO.getStudentId());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);
        studentVO.setToken(token);

        return Result.success(studentVO);
    }

    @PostMapping
    public Result<List<ITStudent>> getAllStudent() {
        List<ITStudent> list = userService.selectAllMember();
        return Result.success(list);
    }

    @DeleteMapping
    public Result<String> removeStudents(@RequestBody ArrayList<String> students) {
        userService.removeStudents(students);
        return Result.success();
    }

    @PutMapping("{id}")
    @CheckPrimaryKeyRepeat
    public Result<String> updateStudent(@PathVariable String id,
                                        @RequestBody UpdateStudent student) {
        userService.updateStudent(id, student);
        return Result.success();
    }

    @PatchMapping("{id}")
    public Result<String> updatePswd(@RequestBody String resetPswd,
                                     @PathVariable String id) {
        userService.updatePassword(id, resetPswd);
        return Result.success();
    }
}
