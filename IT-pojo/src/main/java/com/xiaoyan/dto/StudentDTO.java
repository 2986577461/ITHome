package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class StudentDTO {

    @NotNull
    private Integer id;

    private Integer studentId;

    private String name;

    private String sex;

    private String major;

    private String password;

    private String className;

    private String academy;

    private String position;
}
