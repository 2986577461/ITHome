package com.xiaoyan.controller.user;

import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.vo.ArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userArticles")
@RequestMapping("user/articles")
@Tag(name = "文章管理")
public class ArticlesController {

    @Resource
    private ArticlesService messageService;

    @GetMapping("all")
    @Operation(summary = "返回所有文章")
    @Cacheable(cacheNames = "articleList")
    public Result<List<ArticleVO>> getAll() {
        List<ArticleVO> list = messageService.getAll();
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "上传文章")
    @CacheEvict(cacheNames = "articleList", allEntries = true)
    public Result<String> upload(@RequestBody @Valid ArticleDTO articleDTO) {
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article,"id");
        messageService.upload(article);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改文章")
    @CacheEvict(cacheNames = "articleList", allEntries = true)
    public Result<String> update(@RequestBody ArticleDTO articleDTO) {
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);

        messageService.update(article);
        return Result.success();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除文章")
    @CacheEvict(cacheNames = "articleList", allEntries = true)
    public Result<String> delete(@PathVariable Integer id) {
        messageService.delete(id);
        return Result.success();
    }
}
