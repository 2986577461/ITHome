package com.xiaoyan.controller.user;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.dto.ResourceDTO;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.utils.AliOssUtil;
import com.xiaoyan.vo.ResourcesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
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

    @GetMapping
    @Operation(summary = "返回所有资料")
    public Result<List<ResourcesVO>> getList() {
        List<ResourcesVO> list = resourcesService.getList();
        return Result.success(list);
    }


    @PostMapping("/upload")
    @Transactional
    public Result<String> upload(@RequestPart("file") MultipartFile file,
                                 @RequestPart("resource") String resourceJson) {
        log.info("文件上传:{},{}", file, resourceJson);

        ObjectMapper objectMapper = new ObjectMapper();

        ResourceDTO resourceDTO = null;
        try {
            resourceDTO = objectMapper.readValue(resourceJson, ResourceDTO.class);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        Resources resources = new Resources();
        BeanUtils.copyProperties(resourceDTO, resources);
        resourcesService.upload(resources);
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
