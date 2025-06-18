package com.xiaoyan.pojo;


import lombok.Data;


import java.time.LocalDateTime;


@Data
public class Resources {

    private Long id;
    private String head;
    private String introduce;
    private String studentName;
    private Long studentId;
    private LocalDateTime releaseDateTime;
}