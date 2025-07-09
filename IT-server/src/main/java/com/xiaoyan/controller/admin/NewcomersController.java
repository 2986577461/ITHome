package com.xiaoyan.controller.admin;

import com.xiaoyan.result.Result;
import com.xiaoyan.service.NewcomersService;
import com.xiaoyan.vo.NewcomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminNewcomers")
@RequestMapping("admin/newcomers")
@AllArgsConstructor
@Tag(name = "新学员管理")
@Validated
public class NewcomersController {

    private NewcomersService memberService;

    @DeleteMapping("{id}")
    @Operation(summary = "拒绝申请")
    public Result<String> refuseNewcomer(@PathVariable @NotNull Integer id) {
        memberService.refuseNewcomer(id);
        return Result.success();
    }

    @PutMapping("{id}")
    @Operation(summary = "同意申请")
    public Result<String> agreeNewcomer(@PathVariable @NotNull Integer id) {
        memberService.agreeNewcomer(id);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "获取所有申请")
    public Result<List<NewcomerVO>> getnewcomers() {
        List<NewcomerVO> list = memberService.getAll();
        return Result.success(list);
    }
}
