package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ToString
public class Article implements Serializable {

    @TableId("id")
    private Long id;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Integer studentId;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Integer type;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String head;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String content;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime releaseDateTime;

    private LocalDateTime updatedDateTime;
}
