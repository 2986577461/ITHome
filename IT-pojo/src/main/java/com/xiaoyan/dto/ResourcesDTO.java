package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Data
@ToString
public class ResourcesDTO {

    @NotBlank
    @Size(max = 100, message = "编码长度不能超过100")
    private String head;

    @NotBlank
    @Size(max = 500, message = "编码长度不能超过500")
    private String introduce;

    @NotNull
    private MultipartFile file;

    @NotNull
    private MultipartFile cover;
}
