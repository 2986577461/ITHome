package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.xiaoyan.conmon.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class Article implements BaseEntity {

    private Long id;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String author;

    @NotNull
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Long authorId;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String type;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String head;

    @NotBlank
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String content;


    // 这个字段通常不应该在更新文章内容时被修改，所以可以忽略
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime releaseDateTime;

    private LocalDateTime updatedDateTime;


    @Override
    public Long getStudentId() {
        return this.authorId;
    }

    @Override
    public void setStudentId(Long studentId) {
        this.authorId = studentId;
    }

    @Override
    public String getStudentName() {
        return this.author;
    }

    @Override
    public void setStudentName(String studentName) {
        this.author = studentName;
    }

    @Override
    public LocalDateTime getCreatedDataTime() {
        return this.releaseDateTime;
    }

    @Override
    public void setCreatedDateTime(LocalDateTime createdDateTime) {
        this.releaseDateTime = createdDateTime;
    }
}
