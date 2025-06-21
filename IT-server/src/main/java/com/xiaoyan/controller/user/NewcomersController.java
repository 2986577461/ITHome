package com.xiaoyan.controller.user;

import com.xiaoyan.dto.NewComerDTO;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.NewcomersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;


@RestController("userNewcomers")
@RequestMapping("user/newcomers")
@AllArgsConstructor
@Tag(name = "新学员管理")
public class NewcomersController {

    private NewcomersService memberService;

    @PostMapping("/apply-join")
    @Operation(summary = "申请加入协会")
    public Result<String> applyJoin(@RequestBody NewComerDTO newComerDTO) {
        Newcomer newcomer = new Newcomer();
        BeanUtils.copyProperties(newComerDTO, newcomer);
        memberService.applyJoin(newcomer);
        return Result.success();
    }
}
