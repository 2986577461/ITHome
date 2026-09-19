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
     * 文件访问地址。
     *
     * <p>正常是 OSS 的裸 URL；OSS 当时不可用时是后端的本地预览路径
     * {@code /user/common/local/{objectName}}，补传完成后那个路径会自动 302 到 OSS。
     * 前端只管把它塞进 {@code <img src>}，两种情况都能显示。</p>
     */
    private String fileUrl;
}