package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Newcomer {

    @TableId
    private Long id;

    private Long studentId;

    private String name;

    private String sex;

    @TableField("class")
    private String claxx;

    private String academy;

    private String introduce;

    private String major;
}
