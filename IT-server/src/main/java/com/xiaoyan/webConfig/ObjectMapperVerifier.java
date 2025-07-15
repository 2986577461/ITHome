// 文件名: ObjectMapperVerifier.java
package com.xiaoyan.webConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component // 标记为 Spring 组件，让它被扫描到
public class ObjectMapperVerifier {

    @Autowired // 自动注入你定义在 RedisConfig 中的 ObjectMapper Bean
    private ObjectMapper objectMapper;

    @PostConstruct // 在 Bean 初始化后执行此方法
    public void verifyObjectMapper() {
        System.out.println("--- 正在验证 ObjectMapper 配置 ---");
        Set<Object> registeredModules = objectMapper.getRegisteredModuleIds();
        System.out.println("已注册的模块 ID: " + registeredModules);

        // 检查是否包含 JavaTimeModule
        boolean hasJavaTimeModule = registeredModules.stream()
                .anyMatch(id -> id.toString().contains("JavaTimeModule"));
        System.out.println("是否已注册 JavaTimeModule: " + hasJavaTimeModule);
        System.out.println("--- ObjectMapper 验证结束 ---");
    }
}