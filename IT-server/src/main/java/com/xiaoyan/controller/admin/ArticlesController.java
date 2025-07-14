package com.xiaoyan.controller.admin;


import com.xiaoyan.result.Result;
import com.xiaoyan.service.ArticlesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController("adminArticles")
@RequestMapping("admin/articles")
@Tag(name = "文章管理")
@Slf4j
public class ArticlesController {

    @Resource
    private ArticlesService messageService;

    @GetMapping
    @Operation(summary = "获取文章总数")
    @Cacheable(value = "articlesCount",key = "'articlesCount'")
    public Result<Long> getCount() {
        log.info("获取文章总数");
        return Result.success(messageService.getCount());
    }

}
