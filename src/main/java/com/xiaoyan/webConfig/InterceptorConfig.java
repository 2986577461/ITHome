package com.xiaoyan.webConfig;

import com.xiaoyan.Interceptor.NewcomersInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Resource
    NewcomersInterceptor newcomersInterceptor;

    @Value("${front-location.request-api}")
    private String location;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
       registry.addInterceptor(newcomersInterceptor)
               .addPathPatterns("/newcomers");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有路径
                .allowedOrigins(location) // 允许的前端地址
                .allowCredentials(true) // 允许携带 Cookie
                .maxAge(3600); // 预检请求缓存时间
    }

}
