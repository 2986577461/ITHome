package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.sql.Date;
import java.sql.Time;
import java.util.StringJoiner;

@Data
public class Article {

    @TableId
    private Integer id;

    private String author;

    private String authorId;

    @NotNull
    private String type;

    @NotNull
    private String head;

    @NotNull
    private String content;

    private Date releaseDate;
    private Time releaseTime;

    private Date updateDate;
    private Time updateTime;

    public Article() {
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", Article.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("content='" + content + "'")
                .add("releaseDate=" + releaseDate)
                .add("releaseTime=" + releaseTime)
                .add("updatedDate=" + updateDate)
                .add("updatedTime=" + updateTime)
                .toString();
    }

   }
