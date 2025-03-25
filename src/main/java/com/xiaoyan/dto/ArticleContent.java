package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleContent {

    @NotNull
    private int id;

    @NotNull
    private String type;

    @NotNull
    private String head;

    @NotNull
    private String content;

}
