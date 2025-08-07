package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatDialogDTO {
    @NotNull
    private Integer senderId;
    @NotNull
    private Integer receiverId;
    @NotBlank
    private String content;
}
