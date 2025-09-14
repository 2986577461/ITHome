package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import com.xiaoyan.baseinterface.HashCacheId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "it_student")
public class Student implements Serializable, HashCacheId {

    @TableId("id")
    private Long id;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private Integer studentId;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String name;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String sex;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String major;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String password;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String className;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String academy;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String position;

    private Integer articleCount;

    private Integer resourceCount;

    @TableLogic
    private Boolean deleted;

    @Override
    public String getCacheId() {
        return String.valueOf(this.studentId);
    }
}