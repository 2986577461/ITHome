package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordDTO {
    @NotBlank
    @Size(max= 30,message="编码长度不能超过30")
    private String password;
}
