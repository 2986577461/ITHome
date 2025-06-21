package com.xiaoyan.controller.user;


import com.xiaoyan.dto.ResourceDTO;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.utils.AliOssUtil;
import com.xiaoyan.vo.FileVO;
import com.xiaoyan.vo.ResourcesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController("userResources")
@RequestMapping("user/resources")
@AllArgsConstructor
@Tag(name = "资料管理")
@Validated
public class ResourcesController {

    private AliOssUtil aliOssUtil;

    private ResourcesService resourcesService;

    @GetMapping("all")
    @Operation(summary = "返回所有资料")
    public Result<List<ResourcesVO>> getList() {
        List<ResourcesVO> list = resourcesService.getList();
        return Result.success(list);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传资料到远程OSS")
    public Result<FileVO> upload(MultipartFile file) throws IOException {
        log.info("文件上传:{}", file);

        String name = file.getOriginalFilename();

        String filePath = aliOssUtil.upload(file.getBytes(),
                UUID.randomUUID()
                        + name.substring(name.lastIndexOf(".")));

        return Result.success(FileVO.builder().fileName(name).fileUrl(filePath).build());
    }

    @PostMapping
    public Result<String> saveResource(@RequestBody ResourceDTO resourceDTO) {
        log.info("保存文章：{}", resourceDTO);
        Resources resources = new Resources();
        BeanUtils.copyProperties(resourceDTO, resources);

        resourcesService.saveResource(resources);

        return Result.success();
    }


}
