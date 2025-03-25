package com.xiaoyan.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaoyan.annotation.RequireAdmin;
import com.xiaoyan.annotation.RequireLogin;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.service.ResourcesService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

@RestController
@RequestMapping("resources")
@CrossOrigin
public class ResourcesController {

    @Resource
    private ResourcesService resourcesService;

    @GetMapping("count")
    @RequireAdmin
    public int getCount() {
        return resourcesService.getCount();
    }

    @GetMapping("cover/{id}")
    ResponseEntity<byte[]> getCover(@PathVariable("id") int id) throws IOException {
        return resourcesService.getCover(id);
    }

    @GetMapping
    ArrayList<Resources> getMessage() {
        return resourcesService.getAll();
    }

    @GetMapping("{id}")
    @RequireLogin
    String getResourceName(@PathVariable("id") int id) {
        return resourcesService.getFileName(id);
    }

    @GetMapping("download/{id}")
    @RequireLogin
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable int id) {
        return resourcesService.download(id);
    }

    @PostMapping
    @RequireLogin
    public boolean upload(@RequestParam("resource") String  uploadResourceJson,
                               @RequestParam("file") MultipartFile file,
                               @RequestParam("cover") MultipartFile cover
    ) throws JsonProcessingException {
       return resourcesService.upload(uploadResourceJson,file,cover);
    }


}
