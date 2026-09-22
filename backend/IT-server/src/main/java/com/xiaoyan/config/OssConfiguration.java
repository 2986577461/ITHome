package com.xiaoyan.config;


import com.aliyun.oss.ClientBuilderConfiguration;
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

    /**
     * 建立连接的上限。正常连 OSS 是几百毫秒的事，给到 3 秒还连不上，
     * 基本可以当成不可用，没必要让请求线程继续等
     */
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;

    /**
     * 连上之后等响应（或写 socket）的上限。
     *
     * <p>没压得更狠是因为上传大文件时写 socket 本来就可能阻塞几秒——
     * 对端接收窗口满了就会这样，属于正常现象，不算故障。</p>
     */
    private static final int SOCKET_TIMEOUT_MILLIS = 10000;

    /**
     * 重试次数。留一次应对瞬时抖动就够：真正长时间的故障不指望 SDK 反复试来扛，
     * 上层有降级（写本地 + 补传）和删除重试，比在这儿一遍遍等强
     */
    private static final int MAX_ERROR_RETRY = 1;

    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssProperties properties) {
        log.info("阿里OssBean对象注入{}", properties);
        String endpoint = properties.getEndpoint();
        String accessKeyId = properties.getAccessKeyId();
        String accessKeySecret = properties.getAccessKeySecret();
        String bucketName = properties.getBucketName();
        OSS ossClient = new OSSClientBuilder()
                .build(endpoint, accessKeyId, accessKeySecret, ossClientConfiguration());
        return new AliOssUtil(endpoint, accessKeyId, accessKeySecret, bucketName, ossClient);
    }

    /**
     * OSS 客户端的超时与重试。
     *
     * <p>不配的话吃的是 SDK 默认值：connectionTimeout 50 秒、socketTimeout 50 秒、
     * maxErrorRetry 3，一次 {@code putObject} 最坏能卡到六分多钟才抛出来，才轮到
     * {@link com.xiaoyan.service.impl.CommonServiceImpl#upload} 的降级分支——
     * 那时候用户早就关页面了，等于没降级。传图接口还是串行传多张，卡起来按张数乘。</p>
     *
     * <p>这几个值不适合做成配置项：调大调小改变的是「多久放弃 OSS」这个设计取舍，
     * 跟部署环境无关。</p>
     */
    private ClientBuilderConfiguration ossClientConfiguration() {
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setConnectionTimeout(CONNECT_TIMEOUT_MILLIS);
        configuration.setSocketTimeout(SOCKET_TIMEOUT_MILLIS);
        configuration.setMaxErrorRetry(MAX_ERROR_RETRY);
        return configuration;
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