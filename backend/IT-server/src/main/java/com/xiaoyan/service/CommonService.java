package com.xiaoyan.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CommonService {

    Long upload(MultipartFile file) throws IOException;

    /** 删除 OSS 文件 + student_file 记录，可批量 */
    void delete(String... objectNames);

    String generatePresignedDownloadUrl(String objectName, long expirationMillis);

}