package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentDTO {

    @NotNull(message = "文章不能为空")
    private Long articleId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最多500字")
    private String content;
}
