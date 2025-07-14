package com.xiaoyan.vo;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiDialogSessionVO implements Serializable {

    private Integer id;

    private String title;

    private LocalDateTime createDateTime;

    private LocalDateTime LastActiveDateTime;

}
