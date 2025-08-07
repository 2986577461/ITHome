package com.xiaoyan.webConfig;

import com.xiaoyan.interceptor.JwtAdminTokenInterceptor;
import com.xiaoyan.interceptor.JwtUserTokenInterceptor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${front-location.request-url}")
    private String[] location;

    @Value("${admit.url}")
    private String[] admitUrl;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtUserTokenInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns(admitUrl);

        registry.addInterceptor(jwtAdminTokenInterceptor)
                .addPathPatterns("/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有路径
        registry.addMapping("/**")
                // 允许的前端地址
                .allowedOrigins(location)
                .allowedMethods("*")
                // 允许携带 Cookie
                .allowCredentials(true)
                // 预检请求缓存时间
                .maxAge(3600);
    }
}
