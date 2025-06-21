package com.xiaoyan.pojo;


import com.xiaoyan.conmon.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import java.time.LocalDateTime;


@Data
public class Resources implements BaseEntity {

    private Long id;

    @NotBlank
    private String head;

    @NotBlank
    private String introduce;

    @NotBlank
    private String fileUrl;

    @NotBlank
    private String fileName;

    @NotBlank
    private String coverUrl;

    @NotBlank
    private LocalDateTime releaseDateTime;

    private Long studentId;

    private String studentName;

    @Override
    public LocalDateTime getCreatedDataTime() {
        return this.releaseDateTime;
    }

    @Override
    public void setCreatedDateTime(LocalDateTime createdDateTime) {
        this.releaseDateTime = createdDateTime;
    }

    @Override
    public LocalDateTime getUpdatedDateTime() {
        return null;
    }

    @Override
    public void setUpdatedDateTime(LocalDateTime updatedDateTime) {

    }
}