package com.xiaoyan.service.impl;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.dto.ArticleContent;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.exception.PositionException;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.pojo.ITStudent;
import com.xiaoyan.service.ArticlesService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

@Service
public class ArticlesServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticlesService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Override
    public int getCount() {
        return (int) this.count();
    }


    @Override
    public boolean upload(Article article) {
        ITStudent student = userMapper.selectById((String) StpUtil.getLoginId());
        if (student == null)
            throw new ParameterException("参数异常");

        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        article.setAuthor(student.getName());
        article.setAuthorId(student.getStudentId());
        article.setReleaseDate(Date.valueOf(nowDate));
        article.setReleaseTime(Time.valueOf(nowTime));
        article.setUpdateDate(Date.valueOf(nowDate));
        article.setUpdateTime(Time.valueOf(nowTime));

        return articleMapper.insert(article) == 1;
    }

    @Override
    public ArrayList<Article> getAll() {

        ArrayList<Article> articles =
                (ArrayList<Article>) articleMapper.selectList(null);
        articles.sort((o1, o2) ->
        {
            long time1 = o1.getReleaseDate().getTime() + o1.getReleaseTime().getTime();
            long time2 = o2.getReleaseDate().getTime() + o2.getReleaseTime().getTime();
            return (int) (time2 - time1);
        });
        return articles;
    }

    @Override
    public void update(int id, ArticleContent articleContent) {

        ITStudent student = userMapper.selectById((String) StpUtil.getLoginId());
        Article oldArticle = articleMapper.selectById(id);
        //如果不是会长或者副会长并且文章不是自己的，则不允许更新文章
        if (!(student.getPosition().equals("会长") ||
                student.getPosition().equals("副会长")) &&
                !oldArticle.getAuthorId().equals(student.getStudentId())
        ) throw new PositionException("有刁民试图破解前端代码以修改别人的文章");

        if (articleMapper.selectById(id) == null)
            throw new RuntimeException("没找到文章");

        LambdaUpdateWrapper<Article> luw = new LambdaUpdateWrapper<>();
        luw.set(Article::getType, articleContent.getType())
                .set(Article::getHead, articleContent.getHead())
                .set(Article::getContent, articleContent.getContent())
                .set(Article::getUpdateDate, Date.valueOf(LocalDate.now()))
                .set(Article::getUpdateTime, Time.valueOf(LocalTime.now()))
                .eq(Article::getId,id);

       articleMapper.update(luw);
    }

    @Override
    public boolean delete(int id) {
        Article article = articleMapper.selectById(id);
        ITStudent student = userMapper.selectById((String) StpUtil.getLoginId());
        if (article == null ||
                (student.getPosition().equals("学员") &&
                        !article.getAuthorId().equals(StpUtil.getLoginId())))
            throw new PositionException("删除文章时身份异常");
        return articleMapper.deleteById(id)==1;


    }


}
