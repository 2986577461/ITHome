package com.xiaoyan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量上传图片的返回结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageVO {

    /**
     * student_file 主键 ID
     */
    private Long studentFileId;

    /**
     * OSS 文件访问 URL
     */
    private String fileUrl;
}