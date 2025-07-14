package com.xiaoyan.vo;


import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class StudentDialogVO implements Serializable {

    private String  studentId;

    private String name;
}
