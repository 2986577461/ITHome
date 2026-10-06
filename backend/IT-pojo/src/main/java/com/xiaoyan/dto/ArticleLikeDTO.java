package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleLikeDTO {

    @NotNull(message = "文章不能为空")
    private Long articleId;
}
