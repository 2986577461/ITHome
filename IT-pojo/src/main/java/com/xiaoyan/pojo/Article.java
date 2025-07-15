package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ToString
public class Article implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Integer studentId;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Integer type;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String head;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String content;

    // 这个字段通常不应该在更新文章内容时被修改，所以可以忽略
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime releaseDateTime;

    private LocalDateTime updatedDateTime;
}
