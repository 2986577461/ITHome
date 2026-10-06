package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentLikeDTO {

    @NotNull(message = "评论不能为空")
    private Long commentId;
}
