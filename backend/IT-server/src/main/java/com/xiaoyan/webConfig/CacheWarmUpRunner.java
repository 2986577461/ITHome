package com.xiaoyan.webConfig;

import com.xiaoyan.service.impl.ArticlesServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
@Slf4j
public class CacheWarmUpRunner implements CommandLineRunner {

    private ArticlesServiceImpl articlesServiceImpl;

    @Override
    public void run(String... args) {
        try {
            articlesServiceImpl.buildLatestCache();
            log.info("文章榜单缓存预热完成");
        } catch (Exception e) {
            // 预热失败不能影响启动：查询接口发现缓存没构建过会自己重建
            log.error("文章榜单缓存预热失败，将在首次查询时重建", e);
        }
    }

}
