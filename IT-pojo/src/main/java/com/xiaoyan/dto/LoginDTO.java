package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LoginDTO {

    @NotNull
    private Integer studentId;

    @NotBlank
    @Size(max= 30,message="编码长度不能超过30")
    private String password;

}
