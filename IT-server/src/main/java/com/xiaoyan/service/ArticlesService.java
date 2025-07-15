package com.xiaoyan.service;

import com.xiaoyan.pojo.Article;
import com.xiaoyan.vo.ArticleVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ArticlesService {

    Long getCount();

    void upload( Article article);

    List<ArticleVO> getAll();

    void delete(Long id);

    void update( Article article);
}
