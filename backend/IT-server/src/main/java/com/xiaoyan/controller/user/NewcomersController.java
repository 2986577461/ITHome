package com.xiaoyan.controller.user;

import cn.hutool.core.bean.BeanUtil;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.NewcomersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xiaoyan.dto.NewComerDTO;
import com.xiaoyan.pojo.Newcomer;


@Slf4j
@RestController("userNewcomers")
@RequestMapping("user/newcomers")
@AllArgsConstructor
@Tag(name = "新学员管理")
public class NewcomersController {

    private NewcomersService memberService;

    @PostMapping
    @Operation(summary = "申请加入协会")
    public Result<String> applyJoin(@RequestBody @Valid @NonNull NewComerDTO newComerDTO) {
        log.info("申请加入协会:{}", newComerDTO);
        memberService.applyJoin(BeanUtil.toBean(newComerDTO, Newcomer.class));
        return Result.success();
    }
}