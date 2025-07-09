package com.xiaoyan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class ArticleDTO {

    @Schema(description = "上传文章不需要id，修改需要")
    private Integer id;

    @NotNull(message="[]不能为空")
    private Integer type;

    @Size(min = 2, max= 100,message="编码长度2-100")
    @NotBlank(message="[]不能为空")
    private String head;

    @Size(min = 10, max= 5000,message="编码长度10-5000")
    @NotBlank(message="[]不能为空")
    private String content;
}
