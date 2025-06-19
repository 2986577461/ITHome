package com.xiaoyan.vo;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class StudentVO {

    private Long id;

    private String name;

    private String position;

    private String academy;

    private String major;

    private String sex;

    private String className;

    private Integer articleCount;

    private Integer resourceCount;

    private String  token;
}
