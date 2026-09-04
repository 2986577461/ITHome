package com.xiaoyan.vo;


import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@ToString
@Data
public class MyArticleVO {

    private Long id;

    private String head;

    private Integer type;

    private LocalDateTime updatedDateTime;
}