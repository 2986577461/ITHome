package com.xiaoyan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDialogVO implements Serializable {

    private Integer id;

    private Integer groupId;

    private Integer senderId;

    private String content;

    private String answer;

    private LocalDateTime createDateTime;
}
