package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFile {

    @TableId(value = "id")
    private Long id;

    private String studentId;

    private String originalName;

    private String objectName;

    /**
     * 文件的可访问地址，前端直接塞 {@code <img src>} 用。
     *
     * <p>上传到 OSS 时是 OSS 的裸 URL；降级到本地时是 {@code /user/common/local/{objectName}}，
     * 补传成功后由定时任务改写成 OSS 地址（旧的那个本地路径会 302 过去，不会坏）。</p>
     */
    private String fileUrl;

    /** {@link com.xiaoyan.constant.StorageConstant}：OSS / LOCAL */
    private String storageType;

    private Long fileSize;

    private String fileType;

    private LocalDateTime createDateTime;

}