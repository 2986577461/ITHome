package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class NewComerDTO {

    @NotNull(message="[]不能为空")
    private String studentId;

    @Size(max= 20,message="编码长度不能超过20")
    @NotBlank(message="[]不能为空")
    private String name;

    @NotBlank(message="[]不能为空")
    private String sex;

    @Size(max= 20,message="编码长度不能超过20")
    @NotBlank(message="[]不能为空")
    private String className;

    @Size(max= 20,message="编码长度不能超过20")
    @NotBlank(message="[]不能为空")
    private String academy;

    @Size(max= 2000,message="编码长度不能超过2000")
    @NotBlank(message="[]不能为空")
    private String introduce;

    @Size(max= 20,message="编码长度不能超过20")
    @NotBlank(message="[]不能为空")
    private String major;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度需为6~30位")
    private String password;
}