package com.xiaoyan.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;
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
public class ResourcesController {

    private ResourcesService resourcesService;

    @GetMapping("count")
    public Result<Long> getCount() {
        return Result.success(resourcesService.getCount());
    }

    @GetMapping("cover/{id}")
    public Result<ResponseEntity<byte[]>> getCover(@PathVariable("id") int id) throws IOException {
        ResponseEntity<byte[]> cover = resourcesService.getCover(id);
        return Result.success(cover);
    }

    @GetMapping
    public Result<List<Resources>> getList() {
        List<Resources> list = resourcesService.getList();
        return Result.success(list);
    }

    @GetMapping("{id}")
    public Result<String> getResourceName(@PathVariable("id") int id) {
        return Result.success(resourcesService.getFileName(id));
    }

    @GetMapping("download/{id}")
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
