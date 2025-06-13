package com.xiaoyan.service;

import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.vo.ArticleVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ArticlesService {

    Integer getCount();

    void upload(ArticleDTO articleDTO);

    List<ArticleVO> getAll();

    void delete(Long id);

    void update(ArticleDTO articleDTO);
}
