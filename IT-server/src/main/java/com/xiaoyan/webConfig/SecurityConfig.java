package com.xiaoyan.webConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 允许所有请求
                )
                .csrf(AbstractHttpConfigurer::disable) // 禁用 CSRF（仅适用于无状态 API）
                .formLogin(AbstractHttpConfigurer::disable) // 禁用表单登录（适用于前后端分离项目）
                .httpBasic(AbstractHttpConfigurer::disable); // 禁用 HTTP Basic 认证
        return http.build();
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}