package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

@Data
public class Harvest implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer studentId;

    private String familiarTechnology;

    private String content;

    private Boolean contactType;

    private String contactWay;
}
