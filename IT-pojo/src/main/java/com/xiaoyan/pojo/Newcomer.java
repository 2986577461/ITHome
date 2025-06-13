package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.StringJoiner;

@Data
public class Newcomer {

    @TableId
    private Integer id;

    private String studentId;

    private String name;

    private String sex;

    @TableField("class")
    private String claxx;

    private String academy;

    private String introduce;

    private String major;


    @Override
    public String toString() {
        return new StringJoiner(", ", Newcomer.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("studentId='" + studentId + "'")
                .add("path='" + name + "'")
                .add("sex='" + sex + "'")
                .add("claxx='" + claxx + "'")
                .add("academy='" + academy + "'")
                .add("introduce='" + introduce + "'")
                .add("major='" + major + "'")
                .toString();
    }
}
