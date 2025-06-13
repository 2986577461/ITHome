package com.xiaoyan.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaoyan.pojo.Resources;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface ResourcesService extends IService<Resources> {

    Long getCount();

    ResponseEntity<Resource> download(int id);

    void uploadFile(MultipartFile file, MultipartFile cover, int id) ;

    void upload(String  uploadResourceJson, MultipartFile file, MultipartFile cover) throws JsonProcessingException;

    ResponseEntity<byte[]> getCover(int id) throws IOException;

    List<Resources> getList();

    String getFileName(int id);
}
