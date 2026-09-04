package com.xiaoyan.webConfig;

import com.xiaoyan.service.impl.ArticlesServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
public class CacheWarmUpRunner implements CommandLineRunner {

    private ArticlesServiceImpl articlesServiceImpl;

    @Override
    public void run(String... args) {
        System.out.println("🚀 缓存预热任务开始...");

//        articlesServiceImpl.buildLatestCache();
        System.out.println("✅ 缓存预热任务完成");
    }

}