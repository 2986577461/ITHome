package com.xiaoyan.pojo;


import java.io.Serializable;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class ChatDialog implements Serializable {

    @NotNull(message = "[]不能为空")
    private Integer id;

    @NotBlank(message = "[]不能为空")
    @Size(max = 20, message = "编码长度不能超过20")
    private String sender;

    @NotBlank(message = "[]不能为空")
    @Size(max = 20, message = "编码长度不能超过20")
    private String receiver;

    @NotBlank(message = "[]不能为空")
    @Size(max = 3000, message = "编码长度不能超过3000")
    private String content;

    @NotNull(message = "[]不能为空")
    private Date createDateTime;

}
