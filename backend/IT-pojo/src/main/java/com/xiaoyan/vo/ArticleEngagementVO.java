package com.xiaoyan.vo;

import lombok.Data;

@Data
public class ArticleEngagementVO {

    private Long articleId;

    private Long likeCount;

    private Boolean liked;

    private Long commentCount;
}
