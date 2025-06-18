package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

import java.util.StringJoiner;

@Data
@ToString
public class StudentDTO {

    @NotNull
    private Long id;

    private String name;

    private String sex;

    private String major;

    private String password;

    private String claxx;

    private String academy;

    private String position;

}
