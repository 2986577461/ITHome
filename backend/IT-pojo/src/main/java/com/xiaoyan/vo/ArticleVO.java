package com.xiaoyan.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@ToString
@Data
public class ArticleVO implements Serializable {

    private Long id;

    private String name;

    private String studentId;

    private Integer type;

    private String avatar;

    private String head;

    private String content;

    private LocalDateTime updatedDateTime;
}
