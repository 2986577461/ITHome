package com.xiaoyan.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewcomerVO {

    private Long id;

    private Long studentId;

    private String name;

    private String sex;

    private String className;

    private String academy;

    private String introduce;

    private String major;

    private LocalDateTime applicationDateTime;
}
