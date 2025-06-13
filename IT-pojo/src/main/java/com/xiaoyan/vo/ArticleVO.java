package com.xiaoyan.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@ToString
@Data
public class ArticleVO implements Serializable {

    public static final long serialVersionUID=1L;

    private Long id;

    private String author;

    private String authorId;

    private String type;

    private String head;

    private String content;

    private LocalDateTime releaseDateTime;
}
