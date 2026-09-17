package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "it_student")
public class Student implements Serializable {

    @TableId("id")
    private Long id;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String studentId;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String name;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String sex;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String major;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String password;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String className;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String academy;

    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String position;

    private Long avatarId;

    /**
     * 入会时间：申请被审批通过、这条记录被创建的那一刻。
     *
     * <p>历史成员没有这个数据，值为 null——批准制改造之前，
     * newcomer 行在审批时就被物理删掉了，入会时间无处可查。</p>
     */
    private LocalDateTime createDateTime;

    @TableLogic
    private Boolean deleted;
}