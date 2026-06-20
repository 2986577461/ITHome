package com.xiaoyan.controller.user;

import cn.hutool.core.bean.BeanUtil;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.StudentDTO;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;
import com.xiaoyan.vo.StudentVO;

import java.io.IOException;


@RestController("userUser")
@RequestMapping("user/users")
@Slf4j
@AllArgsConstructor
@Tag(name = "自身信息维护以及登录操作")
public class UsersController {

    private UsersService userService;

    @PostMapping("avatar")
    public Result<String> uploadAvatar(MultipartFile avatar) throws IOException {
        log.info("头像上传");
        userService.uploadAvatar(avatar);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "返回当前学生信息")
    public Result<StudentVO> getUser() {
        Integer studentId = BaseContext.getCurrentStudentId();
        log.info("用户信息回显:{}", studentId);
        StudentVO user = userService.getUser(studentId);
        return Result.success(user);
    }

    @PostMapping("login")
    @Operation(summary = "账号密码登录")
    public Result<StudentVO> login(@RequestBody @Valid LoginDTO message) {
        log.info("用户登录{}", message);
        return userService.login(message);
    }

    @PutMapping
    @Operation(summary = "修改信息")
    public Result<String> updateStudent(@RequestBody @Valid StudentDTO studentDTO) {
        userService.update(BeanUtil.toBean(studentDTO, Student.class));
        return Result.success();
    }
}