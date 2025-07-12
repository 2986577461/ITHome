package com.xiaoyan.controller.user;

import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("user/common")
@AllArgsConstructor
@Tag(name = "公共组件")
public class CommonController {

    private CommonService commonService;


    /**
     * 获取带签名的文件下载URL
     * 前端通过这个URL直接从OSS下载文件，并能显示正确的文件名
     *
     * @param objectName   OSS上的文件路径/名称 (e.g., uploads/uuid-xxx.png)
     * @return 包含预签名URL的响应
     */
    @GetMapping("/url")
    @Operation(summary = "获取带签名的url下载链接")
    public Result<String> getSignedDownloadUrl(String objectName) {
        long expirationMillis = 60 * 1000;
        String signedUrl = commonService.generatePresignedDownloadUrl(
                objectName,  expirationMillis);
        return Result.success(signedUrl);
    }
}
