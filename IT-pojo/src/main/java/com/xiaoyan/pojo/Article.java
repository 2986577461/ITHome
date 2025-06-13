package com.xiaoyan.pojo;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class Article {

    private Long id;

    private String author;

    private String authorId;

    private String type;

    private String head;

    private String content;

    private LocalDateTime releaseDateTime;

    private LocalDateTime updateDateTime;
}
