package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiDialogSession {

    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;

    private String title;

    private Integer studentId;

    private LocalDateTime createDateTime;

    private LocalDateTime LastActiveDateTime;

    @TableLogic
    private Integer deleted;
}
