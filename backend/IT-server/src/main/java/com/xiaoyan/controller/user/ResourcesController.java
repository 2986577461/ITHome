package com.xiaoyan.controller.user;


import com.xiaoyan.context.BaseContext;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.vo.MyResourceVO;
import com.xiaoyan.vo.ResourcesVO;

import java.io.IOException;
import java.util.List;

@RestController("userResources")
@RequestMapping("user/resources")
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

    @GetMapping("all")
    @Operation(summary = "返回所有资料")
    public Result<List<ResourcesVO>> getList() {
        List<ResourcesVO> list = resourcesService.getList();
        return Result.success(list);
    }

    @GetMapping("my")
    @Operation(summary = "返回我的资料")
    public Result<List<MyResourceVO>> getMyResources() {
        String studentId = BaseContext.getCurrentStudentId();
        List<MyResourceVO> list = resourcesService.getMyResources(studentId);
        return Result.success(list);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除资料")
    public Result<String> deleteByid(@PathVariable Long id) {
        String studentId = BaseContext.getCurrentStudentId();
        resourcesService.deleteById(id, studentId);
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "上传资料")
    public Result<String> saveResource(@ModelAttribute @Valid ResourcesDTO resourcesDTO) throws IOException {
        resourcesService.saveResource(resourcesDTO);
        return Result.success();
    }

}