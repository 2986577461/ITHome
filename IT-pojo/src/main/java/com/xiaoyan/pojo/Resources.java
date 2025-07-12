package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Resources implements Serializable {

    private Integer id;

    private String head;

    private String introduce;

    private Integer studentFileCoverId;

    private Integer studentFileFileId;

    private LocalDateTime releaseDateTime;

    private Integer studentId;

    @TableLogic
    private Boolean deleted;
}