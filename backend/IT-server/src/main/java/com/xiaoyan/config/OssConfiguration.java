package com.xiaoyan.config;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xiaoyan.properties.AliOssProperties;
import com.xiaoyan.utils.AliOssUtil;
import io.lettuce.core.ClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yuchao
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(AliOssProperties.class)
public class OssConfiguration {

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
    @Bean
    LettuceClientConfigurationBuilderCustomizer disconnectFast() {
        return builder -> builder.clientOptions(
                ClientOptions.builder()
                        // 断连时直接拒绝命令,不要排队等超时
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .autoReconnect(true)
                        .build());
    }
}