package com.xiaoyan.conmon;


import java.time.LocalDateTime;

public interface BaseEntity {
    Long getStudentId();

    void setStudentId(Long studentId);

    String getStudentName();

    void setStudentName(String studentName);

    // 时间戳
    LocalDateTime getCreatedDataTime();

    void setCreatedDateTime(LocalDateTime createdDateTime);

    LocalDateTime getUpdatedDateTime();

    void setUpdatedDateTime(LocalDateTime updatedDateTime);
}
