package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewComerDTO {

    @NotNull(message="[]不能为空")
    private Integer studentId;

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
}
