package com.xiaoyan.utils;

import com.xiaoyan.properties.LocalFileProperties;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OSS 不可用时的本地兜底存储。
 *
 * <p>文件名直接用 {@code object_name}——它本来就是 {@code student_file} 上 unique 的 UUID，
 * 再加一列 local_path 存本地路径是纯冗余。</p>
 *
 * <p><b>这个类是路径安全的边界。</b>{@code objectName} 有两个来路都不可信：
 * 上传时由用户文件名拼出来，下载时直接来自请求参数。所以 {@link #resolve} 里必须做
 * 「规范化之后仍在根目录内」的校验，不能只靠调用方过滤。</p>
 *
 * <p>目录解析放在 {@link #afterPropertiesSet} 而不是构造器里：{@code @ConfigurationProperties}
 * 是在 Bean 构造完之后才注入的，构造器里读到的会是 null。</p>
 */
@Component
@Slf4j
public class LocalFileStorage implements InitializingBean {

    private final LocalFileProperties properties;

    private Path baseDir;

    public LocalFileStorage(LocalFileProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        String localDir = properties.getLocalDir();
        if (localDir == null || localDir.isBlank()) {
            throw new IllegalStateException("未配置 xiaoyan.file-storage.local-dir，OSS 降级无处可写");
        }
        baseDir = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("创建本地文件目录失败: " + baseDir, e);
        }
        // 相对路径按进程工作目录解析，IDE 里跑和打包跑结果不一样，必须打出来
        log.info("本地兜底文件目录: {}", baseDir);
    }

    /**
     * 把 objectName 解析成绝对路径。
     *
     * @return 合法路径；objectName 为空、或规范化之后跑到根目录外面时返回 null
     */
    public Path resolve(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        Path candidate = baseDir.resolve(objectName).normalize();
        if (!candidate.startsWith(baseDir)) {
            log.warn("拒绝越界的本地文件路径: {}", objectName);
            return null;
        }
        return candidate;
    }

    /** 写入本地。目录不存在会补建，文件已存在直接覆盖 */
    public void save(@NonNull String objectName, @NonNull byte[] bytes) {
        Path path = resolve(objectName);
        if (path == null) {
            throw new IllegalArgumentException("非法的本地文件名: " + objectName);
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
            log.info("文件已转存本地: {} ({} 字节)", path, bytes.length);
        } catch (IOException e) {
            throw new UncheckedIOException("写本地文件失败: " + path, e);
        }
    }

    /**
     * 读本地文件。给补传任务用；下载走 {@link #resolve} + FileSystemResource 流式输出，
     * 不要把 500MB 的文件整个读进内存。
     *
     * @return 文件内容；文件不存在返回 null
     */
    public byte[] read(@NonNull String objectName) {
        Path path = resolve(objectName);
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("读本地文件失败: " + path, e);
        }
    }

    /**
     * 删除本地文件。
     *
     * @return 是否真的删掉了一个文件；文件本来就不存在时返回 false，不抛异常
     */
    public boolean delete(@NonNull String objectName) {
        Path path = resolve(objectName);
        if (path == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("删本地文件失败: " + path, e);
        }
    }
}
