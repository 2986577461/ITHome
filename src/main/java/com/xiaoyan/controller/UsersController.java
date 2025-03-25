package com.xiaoyan.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.xiaoyan.annotation.CheckPrimaryKeyRepeat;
import com.xiaoyan.annotation.RequireAdmin;
import com.xiaoyan.annotation.RequireLogin;
import com.xiaoyan.annotation.RequireLogout;
import com.xiaoyan.dto.StudentAccount;
import com.xiaoyan.dto.UpdateStudent;
import com.xiaoyan.pojo.ITStudent;
import com.xiaoyan.service.UsersService;
import jakarta.annotation.Resource;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;


//等于ResponseBody+Controller
@RestController

//将请求方法的路径增加共同前缀users，
//这样请求该类的所有方法时路径为：localhost：8080/users/...
@RequestMapping("users")

//使该Controller接收跨域请求，也就是IP地址可以不同，
// 端口号相同即可
@CrossOrigin
public class UsersController {

    //等同与Autowired
    @Resource
    private UsersService userService;

    @PostMapping("{id}")   //接收路径参数为id并交给形参
    @RequireLogin
    public ITStudent getUser(
            @PathVariable("id") String id) {
        return userService.getUser(id);
    }

    @PostMapping("login")
    public boolean login(@RequestBody
                         @Validated StudentAccount message) {
        return userService.login(message);
    }

    @PostMapping("autoLogin")
    public SaResult autoLogin() {
        return StpUtil.isLogin() ?
                SaResult.data(StpUtil.getLoginId()) : null;
    }

    @PatchMapping("logout")
    public void logout() {
        StpUtil.logout();
    }

    @PostMapping
    @RequireAdmin
    public ArrayList<ITStudent> getAllStudent() {
        return userService.selectAllMember();
    }

    @DeleteMapping
    @RequireAdmin   //删除学生需要管理员权限
    @RequireLogout  //删除后需要让被删除者退出登录
    public boolean removeStudents(@RequestBody ArrayList<String> students) {
        return userService.removeStudents(students);
    }

    @PutMapping("{id}")
    @RequireAdmin
    @CheckPrimaryKeyRepeat
    public boolean updateStudent(@PathVariable String id,
                                 @RequestBody UpdateStudent student) {
        return userService.updateStudent(id, student);
    }

    @PatchMapping("{id}")
    public boolean updatePswd(@RequestBody String resetPswd,
                              @PathVariable String id) {
        return userService.updatePassword(id, resetPswd);
    }
}
