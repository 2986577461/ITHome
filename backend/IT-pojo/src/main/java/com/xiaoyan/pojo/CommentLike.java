package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment_like")
public class CommentLike {

    @TableId("id")
    private Long id;

    private Long commentId;

    private String studentId;

    private LocalDateTime createDateTime;
}
