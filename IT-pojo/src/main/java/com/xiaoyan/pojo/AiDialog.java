package com.xiaoyan.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
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

    @TableId("id")
    private Long id;

    private String content;

    private String senderType;

    private Long sessionId;

    private LocalDateTime createDateTime;

    @TableLogic
    private Boolean deleted;
}
