package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class Article {

    private Long id;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String author;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Long authorId;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String type;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String head;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String content;

    // 这个字段通常不应该在更新文章内容时被修改，所以可以忽略
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime releaseDateTime;

    private LocalDateTime updateDateTime;
}
