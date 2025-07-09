package com.xiaoyan.pojo;


import com.xiaoyan.conmon.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import java.io.Serializable;
import java.time.LocalDateTime;


@Data
public class Resources implements Serializable {

    private Integer id;

    private String head;

    private String introduce;

    private String fileUrl;

    private String fileName;

    private String coverUrl;

    private LocalDateTime releaseDateTime;

    private Integer studentId;
}