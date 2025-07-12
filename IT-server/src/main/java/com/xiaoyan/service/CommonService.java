package com.xiaoyan.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CommonService {

   Integer upload(MultipartFile file) throws IOException;

    void delete(String objectName);

    String generatePresignedDownloadUrl(String objectName, long expirationMillis);

}
