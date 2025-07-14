package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MessageDTO {

    @NotBlank
    @Size(max = 3000, message = "编码长度不能超过3000")
    private String message;

    private Integer sessionId;
}
