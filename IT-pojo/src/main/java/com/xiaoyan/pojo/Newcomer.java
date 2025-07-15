package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Newcomer implements Serializable {

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

}
