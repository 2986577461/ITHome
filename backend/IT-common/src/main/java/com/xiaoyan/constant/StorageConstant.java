package com.xiaoyan.constant;

/**
 * student_file.storage_type 的取值。文件当前存在哪儿，决定下载走哪条路、补传任务要不要管它。
 */
public class StorageConstant {

    /** 已经上传到 OSS，file_url 有值 */
    public static final String OSS = "OSS";

    /** 上传时 OSS 不可用，文件先落在本机，等定时任务补传 */
    public static final String LOCAL = "LOCAL";
}
