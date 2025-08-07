package com.xiaoyan.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.annotation.AutoFillFields;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
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

        userMapper.addArticleCountById(BaseContext.getCurrentStudentId());
    }

    @Override
    public List<ArticleVO> getAll() {
        List<Article> articles = articleMapper.selectList(null);
        List<ArticleVO> articlevos = new ArrayList<>();

        for (Article article : articles) {
            ArticleVO vo = new ArticleVO();
            BeanUtils.copyProperties(article, vo);

            Student student = userMapper.selectByStudentId(article.getStudentId());
            vo.setName(student.getName());
            articlevos.add(vo);
        }
        articlevos.sort((o1, o2) -> o2.getUpdatedDateTime().compareTo(o1.getUpdatedDateTime()));
        return articlevos;
    }

    @Override
    @AutoFillFields(AutoFillFields.OpType.UPDATE)
    public void update(Article article) {
        articleMapper.updateById(article);
    }

    @Override
    public void delete(Long id) {
        Article article = articleMapper.selectById(id);
        if(article==null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        Integer studentId = BaseContext.getCurrentStudentId();

        if(!article.getStudentId().equals(studentId)) {
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);
        }

        articleMapper.deleteById(id);
        userMapper.decreaceArticleCount(studentId);
    }


}