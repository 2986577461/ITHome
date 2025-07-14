package com.xiaoyan.vo;


import lombok.Data;

import java.io.Serializable;

@Data
public class HarvestVO implements Serializable {

    private Long id;

    private Long studentId;

    private String name;

    private String className;

    private String familiarTechnology;

    private String content;

    private String contactWay;

    private String contactType;
}
