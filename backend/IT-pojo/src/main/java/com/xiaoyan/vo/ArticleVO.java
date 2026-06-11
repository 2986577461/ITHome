package com.xiaoyan.vo;

import com.xiaoyan.baseinterface.ZsetScore;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@ToString
@Data
public class ArticleVO implements Serializable, ZsetScore {

    private Long id;

    private String name;

    private Integer studentId;

    private Integer type;

    private String avatar;

    private String head;

    private String content;

    private LocalDateTime updatedDateTime;

    @Override
    public double getScore() {
        return updatedDateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }
}