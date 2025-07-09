package com.xiaoyan.controller.admin;

import com.xiaoyan.dto.StudentDTO;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.UsersService;

import com.xiaoyan.vo.StudentVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController("adminUser")
@RequestMapping("admin/users")
@Slf4j
@AllArgsConstructor
@Tag(name = "用户管理")
public class UsersController {

    private UsersService userService;

    @GetMapping("all")
    @Operation(summary = "返回所有学生信息")
    public Result<List<StudentVO>> getAll() {
        List<StudentVO> list = userService.getAll();
        return Result.success(list);
    }

    @GetMapping("excel")
    @Operation(summary = "下载学员花名册")
    public ResponseEntity<byte[]> downloadExcel() throws IOException {
        return userService.downloadExcel();
    }


    @DeleteMapping
    @Operation(summary = "删除学生,记得清理localStorage")
    public Result<String> removeStudents(@RequestBody List<Integer> ids) {
        //todo JWT黑名单
        userService.removeStudents(ids);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改信息")
    public Result<String> updateStudent(@RequestBody @Valid StudentDTO studentDTO) {

        Student student = new Student();
        BeanUtils.copyProperties(studentDTO, student);
        userService.update(student);
        return Result.success();
    }
}
