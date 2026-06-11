package com.xiaoyan.vo;

import com.xiaoyan.baseinterface.HashCacheId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ResourcesVO implements Serializable, HashCacheId {

    private Long id;

    private String head;

    private String introduce;

    private Integer studentId;

    private LocalDateTime releaseDateTime;

    private String fileName;

    private String objectName;

    private String avatar;

    private String fileUrl;

    private String coverUrl;

    private String studentName;

    @Override
    public String getCacheId() {
        return String.valueOf(this.id);
    }
}