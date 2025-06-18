package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @NotNull
    private Long id;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String name;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String sex;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String major;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String password;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String claxx;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String academy;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String position;

    private Integer articleCount;

    private Integer resourceCount;
}
