package com.xiaoyan.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ResourcesVO implements Serializable {

    private Long id;

    private String head;

    private String introduce;

    private Integer studentId;

    private LocalDateTime releaseDateTime;

    private String fileName;

    private String objectName;

    private String fileUrl;

    private String coverUrl;

    private String studentName;
}
