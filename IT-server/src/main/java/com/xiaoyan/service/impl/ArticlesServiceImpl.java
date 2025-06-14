package com.xiaoyan.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.exception.PositionException;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.vo.ArticleVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class ArticlesServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticlesService {

    private ArticleMapper articleMapper;

    @Override
    public Long getCount() {
        return articleMapper.selectCount(null);
    }


    @Override
    public void upload(ArticleDTO articleDTO) {
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);
        article.setReleaseDateTime(LocalDateTime.now());
        article.setUpdateDateTime(LocalDateTime.now());

        articleMapper.insert(article);
    }

    @Override
    public List<ArticleVO> getAll() {
        List<Article> articles = articleMapper.selectList(null);
        List<ArticleVO> articleVOS = new ArrayList<>();

        for (Article article : articles) {
            ArticleVO vo = new ArticleVO();
            BeanUtils.copyProperties(article, vo);
            articleVOS.add(vo);
        }
        articleVOS.sort(Comparator.comparing(ArticleVO::getReleaseDateTime));

        return articleVOS;
    }

    @Override
    public void update(ArticleDTO articleDTO) {
        if (!BaseContext.getCurrentId().equals(articleDTO.getAuthorId()))
            throw new PositionException(MessageConstant.OLEY_MODIFY_YOUR_OWN_ARTICLE);
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);
        article.setUpdateDateTime(LocalDateTime.now());

        articleMapper.updateById(article);
    }

    @Override
    public void delete(Long id) {
        Article article = articleMapper.selectById(id);
        if (article.getAuthorId() != BaseContext.getCurrentId())
            throw new PositionException(MessageConstant.OLEY_DELETE_YOUR_OWN_ARTICLE);

        articleMapper.deleteById(id);
    }


}
