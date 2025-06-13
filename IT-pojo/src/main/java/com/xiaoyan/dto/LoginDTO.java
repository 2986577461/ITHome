package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.StringJoiner;

@Data
public class LoginDTO {

    @NotNull
    private String username;

    @NotNull
    private String password;

}
