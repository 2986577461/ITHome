package com.xiaoyan.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourcesVO {

    private Long id;

    private String head;

    private String introduce;

    private LocalDateTime releaseDateTime;

    private Long studentId;

    private String studentName;
}
