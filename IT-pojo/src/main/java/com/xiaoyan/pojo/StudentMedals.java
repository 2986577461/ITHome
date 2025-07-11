package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentMedals {

    private Integer id;

    private String head;

    private Integer studentId;

    private Integer grade;

    private Integer studentFileId;

    private LocalDateTime createDateTime;

    @TableLogic
    private Boolean deleted;
}
