package com.xiaoyan.controller.admin;

import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.StudentDTO;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.vo.StudentVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminUser")
@RequestMapping("admin/users")
@Slf4j
@AllArgsConstructor
@Tag(name = "用户管理")
public class UsersController {

    private UsersService userService;

    @GetMapping
    @Operation(summary = "返回所有学生信息")
    public Result<List<StudentVO>> getAll() {
        List<StudentVO> list = userService.getAll();
        return Result.success(list);
    }

    @DeleteMapping
    @Operation(summary = "删除学生,记得清理localStorage")
    public Result<String> removeStudents(List<Long> ids) {
        //todo JWT黑名单
        userService.removeStudents(ids);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改信息")
    public Result<String> updateStudent(@RequestBody StudentDTO studentDTO) {
        Student student = new Student();
        BeanUtils.copyProperties(studentDTO, student);
        userService.update(student);
        return Result.success();
    }
}
