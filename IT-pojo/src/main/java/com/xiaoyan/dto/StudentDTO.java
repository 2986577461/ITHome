package com.xiaoyan.dto;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class StudentDTO {

    private Long id;

    private String name;

    private String sex;

    private String major;

    private String password;

    private String className;

    private String academy;

    private String position;
}
