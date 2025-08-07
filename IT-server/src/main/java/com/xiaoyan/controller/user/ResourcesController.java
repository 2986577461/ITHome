package com.xiaoyan.controller.user;


import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.vo.ResourcesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController("userResources")
@RequestMapping("user/resources")
@AllArgsConstructor
@Tag(name = "资料管理")
@Validated
public class ResourcesController {


    private ResourcesService resourcesService;

    @GetMapping("all")
    @Operation(summary = "返回所有资料")
    public Result<List<ResourcesVO>> getList() {
        log.info("返回所有资料");
        List<ResourcesVO> list = resourcesService.getList();
        return Result.success(list);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除自己的资料")
    public Result<String> deleteByid(@PathVariable Long id) {
        Integer studentId = BaseContext.getCurrentStudentId();
        log.info("用户{}删除自己的资料{}", studentId, id);
        resourcesService.deleteById(id, studentId);
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "上传资料")
    public Result<String> saveResource(@ModelAttribute @Valid ResourcesDTO resourcesDTO) {
        Integer studentId = BaseContext.getCurrentStudentId();
        log.info("用户{}上传文章{}",studentId,resourcesDTO);
        resourcesService.saveResource(resourcesDTO, studentId);
        return Result.success();
    }

}
