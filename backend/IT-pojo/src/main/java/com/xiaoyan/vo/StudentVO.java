package com.xiaoyan.vo;


import com.xiaoyan.baseinterface.HashCacheId;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class StudentVO implements Serializable, HashCacheId {

    private Long id;

    private String studentId;

    private String name;

    private String position;

    private String academy;

    private String major;

    private String sex;

    private String className;

    private Long articleCount;

    private Long resourceCount;

    private String avatar;

    private String token;

    @Override
    public String getCacheId() {
        return String.valueOf(this.studentId);
    }
}