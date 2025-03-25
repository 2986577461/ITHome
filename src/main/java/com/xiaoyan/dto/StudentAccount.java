package com.xiaoyan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.StringJoiner;

@Data
public class StudentAccount {

    @NotNull
    private String id;

    @NotNull
    private String pswd;

    @Override
    public String toString() {
        return new StringJoiner(", ", StudentAccount.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("pswd='" + pswd + "'")
                .toString();
    }
}
