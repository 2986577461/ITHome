package com.xiaoyan.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;


@Data
@ToString
public class StudentMedalsDTO {

    private Integer id;

    @Schema(description = "奖项名称")
    @NotBlank
    private String head;

    @Schema(description = "1院级2校级3市级4省级5全国级")
    @NotNull
    private Integer grade;

    @NotNull
    @Schema(description = "文件数据，上传方式为application/form-data")
    private MultipartFile medalFile;
}
