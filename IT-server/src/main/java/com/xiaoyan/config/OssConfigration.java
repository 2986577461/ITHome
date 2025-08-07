package com.xiaoyan.config;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xiaoyan.properties.AliOssProperties;
import com.xiaoyan.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yuchao
 */
@Configuration
@Slf4j
public class OssConfigration {

    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssProperties properties) {
        log.info("阿里OssBean对象注入{}", properties);
        String endpoint = properties.getEndpoint();
        String accessKeyId = properties.getAccessKeyId();
        String accessKeySecret = properties.getAccessKeySecret();
        String bucketName = properties.getBucketName();
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        return new AliOssUtil(endpoint, accessKeyId, accessKeySecret, bucketName, ossClient);
    }
}
