package com.xiaoyan.service;

import com.xiaoyan.pojo.StudentFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CommonService {

    StudentFile upload(MultipartFile file) throws IOException;

    /**
     * 上传文件。OSS 不可用时自动降级写本机，记录标成 LOCAL，由定时任务补传——
     * 所以这个方法失败只可能是「OSS 和本地盘同时写不进去」。
     */
    StudentFile upload(byte[] bytes, String originalName, String contentType, long size, String studentId);

    /** 删除文件 + student_file 记录，可批量。本地降级文件只删盘，不碰 OSS */
    void delete(String... objectNames);

    /**
     * 下载入口。按文件的 storage_type 决定给 OSS 预签名地址还是本地路径——两者都是能直接
     * 塞进 {@code <a href>} 的 URL，前端不需要区分。
     */
    String generateDownloadUrl(String objectName, long expirationMillis);

    /**
     * 取降级文件的本地访问入口。只有预览接口一个调用方，{@code file_url} 里的路径也走它。
     *
     * <p>三种结果：</p>
     * <ul>
     *   <li>本地副本还在 —— 返回文件句柄，调用方自己流出去</li>
     *   <li>本地副本已被补传任务清掉 —— 返回 {@code redirectTo}（{@code file_url} 里回填好的
     *       OSS 地址），调用方 302 过去。文章正文里烧死的那个本地 URL 靠这条永远能跟到 OSS</li>
     *   <li>查不到记录、也没有可跳转的地址 —— 返回 null，调用方 404</li>
     * </ul>
     */
    LocalAccess accessLocalFile(String objectName);

    /**
     * @param resource     本地文件句柄；走 {@code redirectTo} 时为 null
     * @param redirectTo   该 302 过去的绝对地址；直接读本地文件时为 null
     */
    record LocalAccess(Resource resource, String originalName, String contentType, String redirectTo) {
    }
}
