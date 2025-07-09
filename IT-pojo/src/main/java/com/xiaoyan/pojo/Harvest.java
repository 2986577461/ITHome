package com.xiaoyan.pojo;


import lombok.Data;

import java.io.Serializable;

@Data
public class Harvest implements Serializable {

    private Integer id;

    private Integer studentId;

    private String familiarTechnology;

    private String content;

    private Boolean contactType;

    private String contactWay;
}
