package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.dto.ArticleContent;
import com.xiaoyan.pojo.Article;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;

@Service
public interface ArticlesService extends IService<Article> {

    int getCount();

    boolean upload( @Validated Article articleJson);

    ArrayList<Article> getAll();

    void update(int id, @Validated ArticleContent articleContent);

    boolean delete(int id);
}
