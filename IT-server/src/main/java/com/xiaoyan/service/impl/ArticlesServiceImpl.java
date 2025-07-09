package com.xiaoyan.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.annotation.AutoFillFields;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.pojo.Student;
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

    private UserMapper userMapper;

    @Override
    public Long getCount() {
        return articleMapper.selectCount(null);
    }


    @Override
    @AutoFillFields(AutoFillFields.OpType.INSERT)
    public void upload(Article article) {
        article.setStudentId(BaseContext.getCurrentStudentId());
        article.setReleaseDateTime(LocalDateTime.now());
        article.setUpdatedDateTime(LocalDateTime.now());
        articleMapper.insert(article);
    }

    @Override
    public List<ArticleVO> getAll() {
        List<Article> articles = articleMapper.selectList(null);
        List<ArticleVO> articleVOS = new ArrayList<>();

        for (Article article : articles) {
            ArticleVO vo = new ArticleVO();
            BeanUtils.copyProperties(article, vo);

            Student student = userMapper.selectByStudentId(article.getStudentId());
            vo.setName(student.getName());
            articleVOS.add(vo);
        }
        articleVOS.sort((o1, o2) -> o2.getUpdatedDateTime().compareTo(o1.getUpdatedDateTime()));
        return articleVOS;
    }

    @Override
    @AutoFillFields(AutoFillFields.OpType.UPDATE)
    public void update(Article article) {
        articleMapper.updateById(article);
    }

    @Override
    public void delete(Integer id) {
        articleMapper.deleteById(id);
    }


}
