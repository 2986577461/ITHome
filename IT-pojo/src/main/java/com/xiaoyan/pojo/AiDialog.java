package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableLogic;
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

    @NotBlank(message = "[]不能为空")
    @Size(max = 3000, message = "编码长度不能超过3000")
    private String content;

    @NotBlank(message = "[]不能为空")
    @Size(max = 20, message = "编码长度不能超过20")
    private String senderType;

    @NotNull(message = "[]不能为空")
    private Integer sessionId;

    @NotNull(message = "[]不能为空")
    private LocalDateTime createDateTime;

    @TableLogic
    private Integer deleted;
}
