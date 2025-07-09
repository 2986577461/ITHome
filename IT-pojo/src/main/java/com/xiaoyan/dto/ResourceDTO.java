package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ResourceDTO {

    @NotBlank
    @Size(max= 100,message="编码长度不能超过100")
    private String head;

    @NotBlank
    @Size(max= 500,message="编码长度不能超过500")
    private String introduce;

    @NotBlank
    @Size(max= 50,message="编码长度不能超过50")
    private String fileName;

    @NotBlank
    @Size(max= 300,message="编码长度不能超过300")
    private String coverUrl;

    @NotBlank
    @Size(max= 300,message="编码长度不能超过300")
    private String fileUrl;
}
