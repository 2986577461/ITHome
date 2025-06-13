package com.xiaoyan.controller;

import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.vo.ArticleVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("articles")
@Slf4j
public class ArticlesController {

    @Resource
    private ArticlesService messageService;

    @GetMapping
    public Result<Integer> getCount() {
        return Result.success(messageService.getCount());
    }

    @GetMapping("all")
    public Result<List<ArticleVO>> getAll() {
        log.info("加载文章...");
        List<ArticleVO> list = messageService.getAll();
        return Result.success(list);
    }

    @PostMapping
    public Result<String> upload(@RequestBody ArticleDTO articleDTO) {
        log.info("文章上传:{}",articleDTO);
        messageService.upload(articleDTO);
        return Result.success();
    }

    @PutMapping
    public Result<String> update(@RequestBody ArticleDTO articleDTO) {
        log.info("文章修改:{}",articleDTO);
        messageService.update(articleDTO);
        return Result.success();
    }

    @DeleteMapping("{id}")
    public Result<String> delete(@PathVariable Long id) {
        messageService.delete(id);
        return Result.success();
    }
}
