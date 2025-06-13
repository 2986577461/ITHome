package com.xiaoyan.controller;

import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.NewcomersService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("newcomers")
@AllArgsConstructor
@Tag(name = "新学员管理")
public class NewcomersController {

    private NewcomersService memberService;

    @DeleteMapping("{id}")
    public Result<String> refuseNewcomer(@PathVariable int id) {
        memberService.refuseNewcomer(id);
        return Result.success();
    }

    @PutMapping("{id}")
    public Result<String> agreeNewcomer(@PathVariable int id) {
        memberService.agreeNewcomer(id);
        return Result.success();
    }

    @PostMapping("/applyJoin")
    public Result<String> applyJoin(@RequestBody @Validated Newcomer newcomer) {
        memberService.applyJoin(newcomer);
        return Result.success();
    }

    @PostMapping
    public Result<List<Newcomer>> getnewcomers() {
        List<Newcomer> list = memberService.getAllnewcomer();
        return Result.success(list);
    }
}
