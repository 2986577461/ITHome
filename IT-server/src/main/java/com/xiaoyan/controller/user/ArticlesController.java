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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userArticles")
@RequestMapping("user/articles")
@Tag(name = "文章管理")
@Slf4j
public class ArticlesController {

    @Resource
    private ArticlesService messageService;

    @GetMapping("all")
    @Operation(summary = "返回所有文章")
    public Result<List<ArticleVO>> getAll() {
        log.info("返回所有文章");
        List<ArticleVO> list = messageService.getAll();
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "上传文章")
    public Result<String> upload(@RequestBody @Valid ArticleDTO articleDTO) {
        log.info("上传文章：{}",articleDTO);
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article,"id");
        messageService.upload(article);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改文章")
    public Result<String> update(@RequestBody ArticleDTO articleDTO) {
        log.info("修改文章:{}",articleDTO);
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);
        messageService.update(article);
        return Result.success();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除文章")
    public Result<String> delete(@PathVariable Integer id) {
        log.info("删除文章:{}",id);
        messageService.delete(id);
        return Result.success();
    }
}
