package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "it_student")
public class Student {

    @NotNull
    private Long id;

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


}
