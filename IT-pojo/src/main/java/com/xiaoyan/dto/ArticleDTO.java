package com.xiaoyan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class ArticleDTO {

    @Schema(description = "上传文章不需要id，修改需要")
    private Long id;

    private String type;

    private String head;

    private String content;
}
