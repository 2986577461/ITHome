package com.xiaoyan.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MyResourceVO implements Serializable {

    private Long id;

    private String head;

    private String introduce;

    private String coverUrl;

    private LocalDateTime releaseDateTime;
}
