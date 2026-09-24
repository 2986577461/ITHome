package com.xiaoyan.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@ToString
@Data
public class ArticleVO implements Serializable {

    private Long id;

    private String name;

    private String studentId;

    private Integer type;

    private String avatar;

    private String head;

    /** 列表预览，平文，不超过两百字。完整正文走单独接口。 */
    private String excerpt;

    private LocalDateTime updatedDateTime;
}
