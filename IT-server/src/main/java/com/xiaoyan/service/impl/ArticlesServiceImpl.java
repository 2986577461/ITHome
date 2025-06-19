package com.xiaoyan.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.annotation.AutoFillFields;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.vo.ArticleVO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
    @AutoFillFields(AutoFillFields.OpType.INSERT)
    public void upload(@Valid Article article) {
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
    @AutoFillFields(AutoFillFields.OpType.UPDATE)
    public void update(@Valid Article article) {
        articleMapper.updateById(article);
    }

    @Override
    public void delete(Long id) {
        articleMapper.deleteById(id);
    }


}
