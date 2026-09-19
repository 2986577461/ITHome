package com.xiaoyan.service.impl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.StorageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.utils.AliOssUtil;
import com.xiaoyan.utils.LocalFileStorage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * 文件存取。全项目唯一碰 OSS 的地方。
 *
 * <p>OSS 是「可降级依赖」：上传时它挂了就写本机并把记录标成
 * {@link StorageConstant#LOCAL}，由 {@code OssSyncTask} 定时补传；下载时按记录的
 * storage_type 决定给 OSS 预签名地址还是后端本地流。见
 * {@link #upload(byte[], String, String, long, String)} 和
 * {@link #generateDownloadUrl(String, long)}。</p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class CommonServiceImpl implements CommonService {

    /**
     * 降级文件的访问路径，存进 {@code file_url}。
     *
     * <p>为什么 file_url 存本地路径而不是留 null：{@code <img src>} 直接吃这一列，
     * 封面、头像、文章配图都指望它；而且 {@code UploadArticle.vue} 是把 file_url
     * 烧进正文 HTML 存库的，留 null 那张图就永久坏掉，OSS 恢复了也不会自愈。</p>
     *
     * <p>补传成功后这一列会被改写成 OSS 地址，但正文里烧死的那个本地 URL 由
     * {@code /user/common/local/{objectName}} 302 过去。</p>
     */
    private static final String LOCAL_FILE_PATH = "/user/common/local/";

    /** 强制按附件下载。不加的话图片类型会内联，点「下载」变成在浏览器里打开 */
    private static final String FORCE_DOWNLOAD_FLAG = "?download=1";

    private static final int MAX_SUFFIX_LENGTH = 16;

    private AliOssUtil aliOssUtil;

    private StudentFileMapper studentFileMapper;

    private LocalFileStorage localFileStorage;

    @Override
    public StudentFile upload(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        return upload(file.getBytes(), originalName, file.getContentType(), file.getSize(),
                BaseContext.getCurrentStudentId());
    }

    @Override
    public StudentFile upload(byte[] bytes, String originalName, String contentType, long size, String studentId) {
        if (originalName == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        String objectName = buildObjectName(originalName);
        String fileUrl = null;
        String storageType = StorageConstant.OSS;
        try {
            fileUrl = aliOssUtil.upload(bytes, objectName);
        } catch (RuntimeException e) {
            // AliOssUtil 的契约就是「任何 OSS 失败都抛 RuntimeException」，这个 try 里
            // 只有那一个调用，所以 catch RuntimeException 是准的。
            // 降级而不是往上抛：抛出去等于用户传的东西直接没了，而 OSS 只是存放地之一。
            log.warn("OSS 上传失败，转存本地待补传 objectName={} 原因={}", objectName, e.getMessage());
            localFileStorage.save(objectName, bytes);
            storageType = StorageConstant.LOCAL;
            fileUrl = LOCAL_FILE_PATH + objectName;
        }

        StudentFile record = StudentFile.builder().
                studentId(studentId).
                fileSize(size).
                originalName(originalName).
                objectName(objectName).
                fileType(contentType).
                createDateTime(LocalDateTime.now()).
                storageType(storageType).
                fileUrl(fileUrl).build();

        studentFileMapper.insert(record);
        return record;
    }

    @Override
    public void delete(String... objectNames) {
        if (objectNames == null || objectNames.length == 0) {
            return;
        }
        List<String> list = Arrays.asList(objectNames);

        List<String> localNames = studentFileMapper.selectByObjectNames(list).stream()
                .filter(file -> StorageConstant.LOCAL.equals(file.getStorageType()))
                .map(StudentFile::getObjectName)
                .toList();

        // 顺序要紧：下面那步 OSS 批量删除失败会抛异常，反过来的话已经降级的那些
        // 本地文件就永远留在盘上没人清了
        localNames.forEach(localFileStorage::delete);

        List<String> ossNames = list.stream()
                .filter(name -> !localNames.contains(name))
                .toList();
        if (!ossNames.isEmpty()) {
            aliOssUtil.deleteObjects(ossNames);
        }

        studentFileMapper.deleteByObjectNames(list);
    }

    @Override
    public String generateDownloadUrl(String objectName, long expirationMillis) {
        StudentFile studentFile = studentFileMapper.selectbyObjectName(objectName);
        if (studentFile == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        log.info("下载文件:{}", studentFile.getOriginalName());

        if (StorageConstant.LOCAL.equals(studentFile.getStorageType())) {
            // 还没补传到 OSS，交给后端自己流出去。返回相对路径而不是绝对地址：
            // 开发环境 /user 走 vite proxy，生产环境是同域 Nginx 路由，
            // 两边都不需要后端知道自己的域名。
            // 这里不看 file_url 而是现拼，免得理论上的 null 值漏到前端去
            return LOCAL_FILE_PATH + objectName + FORCE_DOWNLOAD_FLAG;
        }

        return aliOssUtil.getDownloadUrl(objectName, studentFile.getOriginalName(), expirationMillis);
    }

    @Override
    public LocalAccess accessLocalFile(String objectName) {
        StudentFile studentFile = studentFileMapper.selectbyObjectName(objectName);
        if (studentFile == null) {
            return null;
        }

        // 不看 storage_type，看盘上有没有文件：补传成功后本地副本可能因为删除失败还留着，
        // 那就直接读它，字节是一样的
        Path path = localFileStorage.resolve(objectName);
        if (path != null && Files.isRegularFile(path)) {
            return new LocalAccess(new FileSystemResource(path), studentFile.getOriginalName(),
                    studentFile.getFileType(), null);
        }

        // 本地副本已经被补传任务清掉了。文章正文里烧死的那个本地 URL 靠这里跟到 OSS 去，
        // 所以不能简单地 404。
        String fileUrl = studentFile.getFileUrl();
        if (fileUrl != null && fileUrl.startsWith("http")) {
            return new LocalAccess(null, studentFile.getOriginalName(), studentFile.getFileType(), fileUrl);
        }

        // file_url 还是本地路径（补传没成功），或者压根没值——没有可以跳过去的地方。
        // 这里必须挡住，否则会 302 到自己，无限重定向
        log.warn("本地降级文件不存在且无处可跳: {}", objectName);
        return null;
    }

    private String buildObjectName(String originalName) {
        return UUID.randomUUID() + sanitizeSuffix(originalName);
    }

    /**
     * 取扩展名。
     *
     * <p>原来是直接 {@code substring(lastIndexOf("."))}，而 originalName 完全由用户控制：
     * {@code "a.../../x"} 取出来的「后缀」自带斜杠和 {@code ..}。写 OSS 时 object key 里
     * 带这些无所谓，但降级写本机磁盘时就是一次路径穿越。所以只留字母数字并截断。</p>
     */
    private String sanitizeSuffix(String originalName) {
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return "";
        }
        String suffix = originalName.substring(dotIndex + 1).replaceAll("[^A-Za-z0-9]", "");
        if (suffix.isEmpty()) {
            return "";
        }
        if (suffix.length() > MAX_SUFFIX_LENGTH) {
            suffix = suffix.substring(0, MAX_SUFFIX_LENGTH);
        }
        return "." + suffix;
    }
}
