package com.xiaoyan.pojo;


import java.io.Serializable;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


@Data
public class ChatDialog implements Serializable {

    @TableId("id")
    private Long id;

    private Integer senderId;

    private Integer receiverId;

    private String content;

    private LocalDateTime createDateTime;

}
