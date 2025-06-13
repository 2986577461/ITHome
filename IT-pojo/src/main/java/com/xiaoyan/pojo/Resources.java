package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.TableId;
import com.xiaoyan.dto.ResourceContent;
import lombok.Data;

import java.sql.Time;
import java.sql.Date;


@Data
public class Resources {

    @TableId
    private Integer id;
    private String head;
    private String introduce;
    private String fileName;
    private String studentName;
    private String studentId;
    private Date releaseDate;
    private Time releaseTime;


    public Resources() {

    }

    public void insertContent(ResourceContent resourceContent) {

        this.setHead(resourceContent.getHead());
        this.setIntroduce(resourceContent.getIntroduce());
        this.setStudentId(resourceContent.getStudentId());
        this.setStudentName(resourceContent.getName());
    }


}