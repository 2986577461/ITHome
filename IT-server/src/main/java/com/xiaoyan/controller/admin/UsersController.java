package com.xiaoyan.controller.admin;

import com.xiaoyan.dto.StudentDTO;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommonService;
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

    private CommonService commonService;

    @GetMapping("all")
    @Operation(summary = "返回所有学生信息")
    public Result<List<StudentVO>> getAll() {
        log.info("返回所有学生信息");
        List<StudentVO> list = userService.getAll();
        return Result.success(list);
    }

    @GetMapping("excel")
    @Operation(summary = "下载学员花名册")
    public ResponseEntity<byte[]> downloadExcel() throws IOException {
        log.info("下载花名册");
        return commonService.downloadExcel();
    }


    @DeleteMapping
    @Operation(summary = "删除学生,记得清理localStorage")
    public Result<String> removeStudents(@RequestBody List<Long> ids) {
        log.info("删除学生{}",ids);
        userService.removeStudents(ids);
        return Result.success();
    }
    @PutMapping
    @Operation(summary = "修改信息")
    public Result<String> updateStudent(@RequestBody @Valid StudentDTO studentDTO) {
        log.info("修改学生{}的信息",studentDTO);
        Student student = new Student();
        BeanUtils.copyProperties(studentDTO, student);
        userService.update(student);
        return Result.success();
    }
}