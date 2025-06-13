package com.xiaoyan.pojo;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class Dialog {

    private String id;

    private String sender;

    private String receiver;

    private String content;

    private Long timestamp;
}
