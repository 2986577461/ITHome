package com.xiaoyan.dto;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class ArticleDTO {
    private Long id;

    private String author;

    private String authorId;

    private String type;

    private String head;

    private String content;

    private LocalDateTime updateDateTime;
}
