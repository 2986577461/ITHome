package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xiaoyan.baseinterface.HashCacheId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Newcomer implements Serializable, HashCacheId {

    @TableId("id")
    private Long id;

    private String studentId;

    private String name;

    private String sex;

    private String className;

    private String academy;

    private String introduce;

    private String major;

    /**
     * 申请时自己设置的密码，存的是 BCrypt 哈希。
     * 管理员查询申请列表的接口直接返回本实体，用 @JsonIgnore 避免把哈希带出去。
     */
    @JsonIgnore
    private String password;

    private LocalDateTime applicationDateTime;

    @Override
    public String getCacheId() {
        return String.valueOf(this.id);
    }
}