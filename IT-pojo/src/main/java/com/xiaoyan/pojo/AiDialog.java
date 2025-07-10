package com.xiaoyan.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class AiDialog implements Serializable {

    private Integer id;

    @NotNull(message = "[]不能为空")
    private Integer senderId;

    @NotBlank(message = "[]不能为空")
    @Size(max = 3000, message = "编码长度不能超过3000")
    private String content;

    @NotBlank(message = "[]不能为空")
    @Size(max = 3000, message = "编码长度不能超过3000")
    private String answer;

    @NotNull(message = "[]不能为空")
    private LocalDateTime createDateTime;
}
