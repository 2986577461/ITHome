package com.xiaoyan.pojo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@ToString
public class AiDialog implements Serializable {

    private Integer id;

    private String sender;

    private String content;

    private LocalDateTime createDateTime;
}
