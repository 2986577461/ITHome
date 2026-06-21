package com.xiaoyan.controller.admin;

import com.xiaoyan.result.Result;
import com.xiaoyan.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xiaoyan.vo.StudentVO;

import java.io.IOException;
import java.util.List;

@RestController("adminUser")
@RequestMapping("admin/users")
@Slf4j
@AllArgsConstructor
@Tag(name = "用户管理")
public class UsersController {

    private UsersService usersService;

    @GetMapping("all")
    @Operation(summary = "返回所有学生信息")
    public Result<List<StudentVO>> getAll() {
        log.info("返回所有学生信息");
        List<StudentVO> list = usersService.getAll();
        return Result.success(list);
    }

    @GetMapping("excel")
    @Operation(summary = "下载学员花名册")
    public ResponseEntity<byte[]> downloadExcel() throws IOException {
        log.info("下载花名册");
        return usersService.downloadExcel();
    }

    @DeleteMapping
    @Operation(summary = "删除学生")
    public Result<String> removeStudents(@RequestBody List<Integer> studentIds) {
        log.info("删除学生{}", studentIds);
        usersService.removeStudents(studentIds);
        return Result.success();
    }
}