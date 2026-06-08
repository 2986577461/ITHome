package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.xiaoyan.baseinterface.HashCacheId;
import com.xiaoyan.baseinterface.ZsetScore;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@ToString
public class Article implements Serializable, HashCacheId , ZsetScore {

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

    @Override
    public String getCacheId() {
        return String.valueOf(this.id);
    }

    @Override
    public double getScore() {
        return updatedDateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }
}