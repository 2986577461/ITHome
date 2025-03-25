package com.xiaoyan.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaoyan.pojo.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

@Service
public interface ResourcesService extends IService<Resources> {

    int getCount();

    ResponseEntity<org.springframework.core.io.Resource> download(int id);

    void uploadFile(MultipartFile file, MultipartFile cover, int id) ;

    boolean upload(String  uploadResourceJson, MultipartFile file, MultipartFile cover) throws JsonProcessingException;

    ResponseEntity<byte[]> getCover(int id) throws IOException;

    ArrayList<Resources> getAll();

    String getFileName(int id);
}
