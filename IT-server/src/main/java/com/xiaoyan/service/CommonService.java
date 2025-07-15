package com.xiaoyan.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CommonService {

    Long upload(MultipartFile file) throws IOException;

    void delete(String objectName);

    String generatePresignedDownloadUrl(String objectName, long expirationMillis);

    ResponseEntity<byte[]> downloadExcel() throws IOException;
}
