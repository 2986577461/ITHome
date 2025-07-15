package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
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

    @TableId("id")
    private Long id;

    private String head;

    private String introduce;

    private Long studentFileCoverId;

    private Long studentFileFileId;

    private LocalDateTime releaseDateTime;

    private Integer studentId;

    @TableLogic
    private Boolean deleted;
}