package com.xiaoyan.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@ToString
@Data
public class ArticleVO implements Serializable {
    
    private Integer id;

    private String name;

    private Integer studentId;

    private Integer type;

    private String head;

    private String content;

    private LocalDateTime updatedDateTime;
}
