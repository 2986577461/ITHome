package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFile {

    @TableId(value = "id")
    private Long id;

    private Integer studentId;

    private String originalName;

    private String objectName;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private LocalDateTime createDateTime;

    @TableLogic
    private Boolean deleted;
}
