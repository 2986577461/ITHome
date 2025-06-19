package com.xiaoyan.pojo;

import com.xiaoyan.conmon.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Newcomer implements BaseEntity {

    private Long id;

    @NotNull
    private Long studentId;

    @NotBlank
    private String name;

    @NotBlank
    private String sex;

    @NotBlank
    private String className;

    @NotBlank
    private String academy;

    @NotBlank
    private String introduce;

    @NotBlank
    private String major;

    private LocalDateTime applicationDateTime;

    @Override
    public String getStudentName() {
        return this.name;
    }

    @Override
    public void setStudentName(String studentName) {
        this.name = studentName;
    }

    @Override
    public LocalDateTime getCreatedDataTime() {
        return this.applicationDateTime;
    }

    @Override
    public void setCreatedDateTime(LocalDateTime createdDateTime) {
        this.applicationDateTime = createdDateTime;
    }

    @Override
    public LocalDateTime getUpdatedDateTime() {
        return null;
    }

    @Override
    public void setUpdatedDateTime(LocalDateTime updatedDateTime) {

    }
}
