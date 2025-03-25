package com.xiaoyan.controller;

import com.xiaoyan.annotation.RequireAdmin;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.service.NewcomersService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("newcomers")
@CrossOrigin
public class NewcomersController {

    @Resource
    private NewcomersService memberService;

    @DeleteMapping("{id}")
    @RequireAdmin
    public boolean refuseNewcomer(@PathVariable int id) {
        return memberService.refuseNewcomer(id);
    }

    @PutMapping("{id}")
    @RequireAdmin
    public boolean agreeNewcomer(@PathVariable int id) {
        return memberService.agreeNewcomer(id);
    }

    @PostMapping("/applyJoin")
    boolean applyJoin(@RequestBody @Validated Newcomer newcomer) {
        return memberService.applyJoin(newcomer);
    }

    @PostMapping
    @RequireAdmin
    ArrayList<Newcomer> getnewcomers() {
        return memberService.getAllnewcomer();
    }
}
