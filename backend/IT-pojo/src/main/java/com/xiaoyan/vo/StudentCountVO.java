package com.xiaoyan.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 「每个学生有多少条记录」的聚合结果，配合 {@code GROUP BY student_id} 使用。
 *
 * <p>文章数和资料数各查一次就能拿到全部学生的，避免在循环里逐个 count。</p>
 */
@ToString
@Data
public class StudentCountVO implements Serializable {

    private String studentId;

    /** 记录条数。注意别用 count 当字段名，容易和 SQL 函数撞上 */
    private Long total;
}
