package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.StringJoiner;

@Data
public class Newcomer {

    @TableId
    private Integer id;

    @NotNull
    private String studentId;

    @NotNull
    private String name;

    @Pattern(regexp = "[男女]", message = "性别必须是'男'或'女'")
    private String sex;

    @NotNull
    @TableField("class")
    private String claxx;

    @NotNull
    private String academy;

    @NotNull
    private String introduce;

    @NotNull
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
