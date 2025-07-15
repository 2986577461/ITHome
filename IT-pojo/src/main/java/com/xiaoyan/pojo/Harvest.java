package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

@Data
public class Harvest implements Serializable {

    @TableId("id")
    private Long id;

    private Integer studentId;

    private String familiarTechnology;

    private String content;

    private Boolean contactType;

    private String contactWay;
}
