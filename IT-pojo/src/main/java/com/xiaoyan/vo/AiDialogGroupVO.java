package com.xiaoyan.vo;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiDialogGroupVO {

    private Integer id;

    private String title;

    private LocalDateTime createDateTime;

    private LocalDateTime LastActiveDateTime;

}
