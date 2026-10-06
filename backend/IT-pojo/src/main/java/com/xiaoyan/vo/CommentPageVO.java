package com.xiaoyan.vo;

import lombok.Data;

import java.util.List;

@Data
public class CommentPageVO {

    private List<CommentVO> records;

    private Long total;

    private Boolean hasMore;
}
