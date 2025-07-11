package com.xiaoyan.controller.user;

import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Slf4j
@RestController("user/common")
@AllArgsConstructor
public class CommonController {

    private CommonService commonService;

//    @PostMapping("/upload")
    @Operation(summary = "上传资料到远程OSS")
    public Result<FileVO> upload(@NotNull MultipartFile file) throws IOException {
        log.info("文件上传:{}", file);
        commonService.upload(file);
        return Result.success();
    }

    //todo暂不注册
//    @DeleteMapping("{objectName}")
    public Result<String> delete(@PathVariable String objectName){
        commonService.delete(objectName);
        return Result.success();
    }
}
