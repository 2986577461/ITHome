package com.xiaoyan.controller;


import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.utils.AliOssUtil;
import com.xiaoyan.vo.ResourcesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("resources")
@AllArgsConstructor
@Tag(name = "资料管理")
@Validated
public class ResourcesController {

    private AliOssUtil aliOssUtil;

    private ResourcesService resourcesService;

    @GetMapping("count")
    @Operation(summary = "获取资料总数")
    public Result<Long> getCount() {
        Long count = resourcesService.getCount();
        return Result.success(count);
    }

    @GetMapping
    @Operation(summary = "返回所有资料")
    public Result<List<ResourcesVO>> getList() {
        List<ResourcesVO> list = resourcesService.getList();
        return Result.success(list);
    }


    @PostMapping("/upload")
    @Transactional
    public Result<String> upload(@RequestPart("file") MultipartFile file,
                                 @RequestPart("resource")String resourceJson) {
        log.info("文件上传:{},{}", file, resourceJson);

        resourcesService.upload(resourceJson);
        try {
            String name = file.getOriginalFilename();

            String filePath = aliOssUtil.upload(file.getBytes(),
                    UUID.randomUUID()
                            + name.substring(name.lastIndexOf(".")));

            return Result.success(filePath);
        } catch (IOException e) {
            log.error(e.toString());
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }


}
