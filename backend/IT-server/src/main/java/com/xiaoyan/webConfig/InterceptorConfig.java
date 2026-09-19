package com.xiaoyan.webConfig;

import com.xiaoyan.interceptor.BaseContextCleanInterceptor;
import com.xiaoyan.interceptor.JwtAdminTokenInterceptor;
import com.xiaoyan.interceptor.JwtUserTokenInterceptor;
import com.xiaoyan.properties.AdmitUrlProperties;
import io.lettuce.core.ClientOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * @author yuchao
 */
@Slf4j
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Resource
    private JwtAdminTokenInterceptor jwtAdminTokenInterceptor;

    @Resource
    private JwtUserTokenInterceptor jwtUserTokenInterceptor;

    @Resource
    private BaseContextCleanInterceptor baseContextCleanInterceptor;

    @Value("${front-location.request-url}")
    private String[] location;

    @Resource
    private AdmitUrlProperties admitUrlProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtUserTokenInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns(admitUrlProperties.getAdmitUrls());

        registry.addInterceptor(jwtAdminTokenInterceptor)
                .addPathPatterns("/admin/**");

        // 放在最后注册，保证所有请求（含免登录白名单）结束后都会清理 ThreadLocal
        registry.addInterceptor(baseContextCleanInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有路径
        registry.addMapping("/**")
                // 允许的前端地址
                .allowedOrigins(location)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 允许携带 Cookie
                .allowCredentials(true)
                // 预检请求缓存时间
                .maxAge(3600);
    }
}