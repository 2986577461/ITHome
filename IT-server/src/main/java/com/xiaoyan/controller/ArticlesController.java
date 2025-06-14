package com.xiaoyan.controller;

import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.vo.ArticleVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("articles")
@Slf4j
@Tag(name = "文章管理")
public class ArticlesController {

    @Resource
    private ArticlesService messageService;

    @GetMapping
    public Result<Long> getCount() {
        return Result.success(messageService.getCount());
    }

    @GetMapping("all")
    @Cacheable(cacheNames = "articleList")
    public Result<List<ArticleVO>> getAll() {
        log.info("加载文章...");
        List<ArticleVO> list = messageService.getAll();
        return Result.success(list);
    }

    @PostMapping
    @CacheEvict(cacheNames = "articleList",allEntries = true)
    public Result<String> upload(@RequestBody @Valid ArticleDTO articleDTO) {
        log.info("文章上传:{}",articleDTO);
        messageService.upload(articleDTO);
        return Result.success();
    }

    @PutMapping
    @CacheEvict(cacheNames = "articleList",allEntries = true)
    public Result<String> update(@RequestBody @Valid ArticleDTO articleDTO) {
        log.info("文章修改:{}",articleDTO);
        messageService.update(articleDTO);
        return Result.success();
    }

    @DeleteMapping("{id}")
    @CacheEvict(cacheNames = "articleList",allEntries = true)
    public Result<String> delete(@PathVariable Long id) {
        messageService.delete(id);
        return Result.success();
    }
}
