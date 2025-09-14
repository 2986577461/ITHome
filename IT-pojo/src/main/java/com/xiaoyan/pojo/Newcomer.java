package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.xiaoyan.baseinterface.HashCacheId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Newcomer implements Serializable, HashCacheId {

    @TableId("id")
    private Long id;

    private Integer studentId;

    private String name;

    private String sex;

    private String className;

    private String academy;

    private String introduce;

    private String major;

    private LocalDateTime applicationDateTime;

    @Override
    public String getCacheId() {
        return String.valueOf(this.id);
    }
}