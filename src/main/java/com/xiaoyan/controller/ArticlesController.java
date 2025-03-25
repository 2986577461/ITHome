package com.xiaoyan.controller;

import com.xiaoyan.annotation.RequireAdmin;
import com.xiaoyan.annotation.RequireLogin;
import com.xiaoyan.dto.ArticleContent;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.service.ArticlesService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("articles")
@CrossOrigin
public class ArticlesController {

    @Resource
    private ArticlesService messageService;

    @GetMapping("count")
    @RequireAdmin
    public int getCount() {
        return messageService.getCount();
    }

    @GetMapping
    ArrayList<Article> getAll() {
        return messageService.getAll();
    }

    @PostMapping
    @RequireLogin
    boolean upload(@RequestBody Article article) {
        return messageService.upload(article);
    }

    @PutMapping("{id}")
    @RequireLogin
    void update(@PathVariable("id")int id,@RequestBody ArticleContent articleContent){
        messageService.update(id,articleContent);
    }

    @DeleteMapping("{id}")
    @RequireLogin
    boolean delete(@PathVariable int id) {
        return messageService.delete(id);
    }
}
