package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailDTO {
    @NotBlank
    private String email;

    @NotBlank
    private String code;
}
