package com.xiaoyan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ResourceDTO {

    @NotBlank
    private String head;
    @NotBlank
    private String introduce;
    @NotBlank
    private String studentName;
    @NotBlank
    private Long studentId;

}
