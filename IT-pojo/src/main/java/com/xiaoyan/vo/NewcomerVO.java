package com.xiaoyan.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class NewcomerVO implements Serializable {

    private Integer id;

    private Integer studentId;

    private String name;

    private String sex;

    private String className;

    private String academy;

    private String introduce;

    private String major;

    private LocalDateTime applicationDateTime;
}
