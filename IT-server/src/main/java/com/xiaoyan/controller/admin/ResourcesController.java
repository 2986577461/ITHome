package com.xiaoyan.controller.admin;


import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController("adminResources")
@RequestMapping("admin/resources")
@AllArgsConstructor
@Tag(name = "资料管理")
@Validated
public class ResourcesController {
    private ResourcesService resourcesService;

    @GetMapping("count")
    @Operation(summary = "获取资料总数")
    public Result<Long> getCount() {
        Long count = resourcesService.getCount();
        return Result.success(count);
    }
}
