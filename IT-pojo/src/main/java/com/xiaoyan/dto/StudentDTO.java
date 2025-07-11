package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class StudentDTO {

    @NotNull
    private Integer id;

    private Integer studentId;

    @Size(max= 20,message="编码长度不能超过20")
    private String name;

    private String sex;

    @Size(max= 20,message="编码长度不能超过20")
    private String major;

    @Size(max= 30,message="编码长度不能超过30")
    private String password;

    @Size(max= 20,message="编码长度不能超过20")
    private String className;

    @Size(max= 20,message="编码长度不能超过20")
    private String academy;

    @Size(max= 20,message="编码长度不能超过20")
    private String position;
}
