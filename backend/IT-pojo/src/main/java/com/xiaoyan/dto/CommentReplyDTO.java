package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReplyDTO {

    @NotNull(message = "评论不能为空")
    private Long commentId;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复最多500字")
    private String content;
}
