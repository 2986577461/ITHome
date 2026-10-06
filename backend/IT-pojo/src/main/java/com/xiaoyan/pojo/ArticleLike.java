package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_like")
public class ArticleLike {

    @TableId("id")
    private Long id;

    private Long articleId;

    private String studentId;

    private LocalDateTime createDateTime;
}
