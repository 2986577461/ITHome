package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_comment")
public class ArticleComment {

    @TableId("id")
    private Long id;

    private Long articleId;

    private Long parentId;

    private String studentId;

    private String content;

    private LocalDateTime createDateTime;

    @TableLogic
    private Integer deleted;
}
