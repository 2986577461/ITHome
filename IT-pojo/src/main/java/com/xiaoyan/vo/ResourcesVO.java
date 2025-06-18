package com.xiaoyan.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourcesVO {

    private Integer id;
    private String head;
    private String introduce;
    private String fileName;
    private String studentName;
    private LocalDateTime releaseDateTime;
}
