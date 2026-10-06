package com.xiaoyan.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;

    private Long articleId;

    private Long parentId;

    private String studentId;

    private String name;

    private String avatar;

    private String content;

    private LocalDateTime createDateTime;

    private Long likeCount;

    private Boolean liked;

    private List<CommentVO> replies;
}
