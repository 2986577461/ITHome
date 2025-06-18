package com.xiaoyan.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.vo.ResourcesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("resources")
@AllArgsConstructor
@Tag(name = "资料管理")
public class ResourcesController {

    private ResourcesService resourcesService;

    @GetMapping("count")
    @Operation(summary = "获取资料总数")
    public Result<Long> getCount() {
        Long count = resourcesService.getCount();
        return Result.success(count);
    }

    @GetMapping("cover/{id}")

    public Result<ResponseEntity<byte[]>> getCover(@PathVariable("id") int id) throws IOException {
        ResponseEntity<byte[]> cover = resourcesService.getCover(id);
        return Result.success(cover);
    }

    @GetMapping
    @Operation(summary = "返回所有资料")
    public Result<List<ResourcesVO>> getList() {
        List<ResourcesVO> list = resourcesService.getList();
        return Result.success(list);
    }

    @GetMapping("{id}")
    public Result<String> getResourceName(@PathVariable("id") int id) {
        return Result.success(resourcesService.getFileName(id));
    }

    @GetMapping("download/{id}")
    @Operation(summary = "下载资料")
    public Result<ResponseEntity<Resource>> download(@PathVariable int id) {
        return Result.success(resourcesService.download(id));
    }

    @PostMapping
    public Result<String> upload(@RequestParam("resource") String uploadResourceJson,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam("cover") MultipartFile cover
    ) throws JsonProcessingException {
        resourcesService.upload(uploadResourceJson, file, cover);
        return Result.success();
    }


}
