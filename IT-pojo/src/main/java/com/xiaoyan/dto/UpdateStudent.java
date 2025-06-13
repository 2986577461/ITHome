package com.xiaoyan.dto;

import lombok.Data;

import java.util.StringJoiner;

@Data
public class UpdateStudent {

    private String studentId;

    private String name;

    private String sex;

    private String major;

    private String password;

    private String claxx;

    private String academy;

    private String position;

    @Override
    public String toString() {
        return new StringJoiner(", ", UpdateStudent.class.getSimpleName() + "[", "]")
                .add("studentId='" + studentId + "'")
                .add("name='" + name + "'")
                .add("sex='" + sex + "'")
                .add("major='" + major + "'")
                .add("password='" + password + "'")
                .add("claxx='" + claxx + "'")
                .add("academy='" + academy + "'")
                .add("position='" + position + "'")
                .toString();
    }
}
