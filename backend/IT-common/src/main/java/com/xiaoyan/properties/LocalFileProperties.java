package com.xiaoyan.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xiaoyan.file-storage")
public class LocalFileProperties {

    /**
     * OSS 不可用时文件的落地目录。
     *
     * <p>相对路径按进程的工作目录解析，IDE 里跑和打包跑结果不一样，
     * 所以 {@code LocalFileStorage} 启动时会把解析后的绝对路径打进日志。</p>
     */
    private String localDir;
}
