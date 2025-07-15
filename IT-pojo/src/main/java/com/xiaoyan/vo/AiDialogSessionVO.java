package com.xiaoyan.vo;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiDialogSessionVO implements Serializable {

    private Long id;

    private String title;

    private LocalDateTime createDateTime;

    private LocalDateTime LastActiveDateTime;

}
