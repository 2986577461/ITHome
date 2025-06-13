package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ResourceContent;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.ITStudent;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.service.ResourcesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;


@Service
public class ResourcesServiceImpl extends ServiceImpl<ResourcesMapper, Resources>
        implements ResourcesService {

    @Value("${resourses-root-directory.path}")
    String filePath;

    @jakarta.annotation.Resource
    private ResourcesMapper resourcesMapper;

    @jakarta.annotation.Resource
    private UserMapper userMapper;

    @Override
    public ResponseEntity<Resource> download(int id) {

        if (resourcesMapper.selectById(id) == null)
            throw new ParameterException("参数异常");

        File[] files = new File(filePath + "/" + id + "/file").listFiles();

        if (files == null || files.length == 0)
            throw new RuntimeException("文件异常");

        FileSystemResource resource = new FileSystemResource(files[0]);

        // 设置响应头，告诉浏览器这是一个文件下载
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }

    @Override
    public void uploadFile(MultipartFile file, MultipartFile cover, int id) {

        File resource = new File(filePath + "/" + id);
        File fileDirectory = new File(resource, "file");
        File coverDirectory = new File(resource, "cover");

        try {
            if (resource.mkdirs() && fileDirectory.mkdirs() && coverDirectory.mkdirs()) {

                file.transferTo(new File(fileDirectory, Objects.requireNonNull(file.getOriginalFilename())));
                cover.transferTo(new File(coverDirectory, Objects.requireNonNull(cover.getOriginalFilename())));
            }
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Long getCount() {
        return this.count();
    }

    @Override
    public List<Resources> getList() {
        return resourcesMapper.selectList(null);
    }

    @Override
    public ResponseEntity<byte[]> getCover(int id) throws IOException {

        File[] coverDirectory = new File(filePath + "/" + id + "/cover").listFiles();

        if (coverDirectory == null || coverDirectory.length == 0)
            throw new RuntimeException("文件异常");

        byte[] imageBytes = Files.readAllBytes(coverDirectory[0].toPath());


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @Override
    public void upload(String uploadResourceJson, MultipartFile file, MultipartFile cover) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        ResourceContent resourceContent = objectMapper.
                readValue(uploadResourceJson, ResourceContent.class);

        ITStudent author = userMapper.selectById(BaseContext.getCurrentId());

        Resources resources = new Resources();

        resources.insertContent(resourceContent);

        resources.setFileName(file.getOriginalFilename());
        resources.setStudentId(author.getStudentId());
        resources.setStudentName(author.getName());

// 使用 LocalDate 和 LocalTime 只设置日期和时间部分
        LocalDate releaseDate = LocalDate.now(); // 获取当前日期
        LocalTime releaseTime = LocalTime.now(); // 获取当前时间

        // 将 LocalDate 转换为 java.sql.Date
        resources.setReleaseDate(Date.valueOf(releaseDate));
        // 将 LocalTime 转换为 java.sql.Time
        resources.setReleaseTime(Time.valueOf(releaseTime));

// 生成唯一 ID
        Random random = new Random();
        int ID = random.nextInt(100000);
        while (resourcesMapper.selectById(ID) != null)
            ID = random.nextInt(100000);

        resources.setId(ID);

// 插入数据
        resourcesMapper.insert(resources);

// 上传文件
        this.uploadFile(file, cover, ID);

        userMapper.addReourceCountByID(BaseContext.getCurrentId());
    }

    @Override
    public String getFileName(int id) {
        return resourcesMapper.selectFileNameById(id);
    }
}
