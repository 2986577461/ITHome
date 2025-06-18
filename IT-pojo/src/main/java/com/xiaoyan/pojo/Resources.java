package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.TableId;
import com.xiaoyan.dto.ResourceContent;
import lombok.Data;

import java.sql.Time;
import java.sql.Date;
import java.time.LocalDateTime;


@Data
public class Resources {
    private Long id;
    private String head;
    private String introduce;
    private String fileName;
    private String studentName;
    private Long studentId;
    private LocalDateTime releaseDateTime;


    public Resources() {

    }

    public void insertContent(ResourceContent resourceContent) {

        this.setHead(resourceContent.getHead());
        this.setIntroduce(resourceContent.getIntroduce());
        this.setStudentId(resourceContent.getStudentId());
        this.setStudentName(resourceContent.getName());
    }


}