package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentMedals {

    @TableId("id")
    private Long id;

    private String head;

    private Integer studentId;

    private Integer grade;

    private Long studentFileId;

    private LocalDateTime createDateTime;

    @TableLogic
    private Boolean deleted;
}
