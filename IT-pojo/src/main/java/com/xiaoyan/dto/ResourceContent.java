package com.xiaoyan.dto;

import lombok.Data;
@Data
public class ResourceContent {

    private String head;

    private String introduce;

    private String studentId;

    private String name;


    @Override
    public String toString() {
        return "Resource{" +
                ", head='" + head + '\'' +
                ", introduce='" + introduce + '\'' +
                ", studentId='" + studentId + '\'' +
                '}';
    }
}
