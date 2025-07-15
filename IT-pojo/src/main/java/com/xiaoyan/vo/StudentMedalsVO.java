package com.xiaoyan.vo;


import lombok.Data;

import java.io.Serializable;

@Data
public class StudentMedalsVO implements Serializable {

    private Long id;

    private String head;

    private Integer studentId;

    private String studentName;

    private String  grade;

    private String medalUrl;

}
