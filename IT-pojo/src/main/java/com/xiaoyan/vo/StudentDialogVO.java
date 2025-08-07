package com.xiaoyan.vo;


import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class StudentDialogVO implements Serializable {

    private Integer studentId;

    private String name;
}
