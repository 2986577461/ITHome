package com.xiaoyan.service.impl;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.ArticleVO;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_COUNT_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;

@Service
@AllArgsConstructor
public class ArticlesServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticlesService {

    private ArticleMapper articleMapper;
    private RedisUtil redisUtil;
    private UserMapper userMapper;
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Long getCount() {
        return redisUtil.queryCountWithLogicalExpire(CACHE_COUNT_ARTICLES, this::count);
    }

    @Override
    public void upload(Article article) {
        article.setStudentId(BaseContext.getCurrentStudentId());
        article.setReleaseDateTime(LocalDateTime.now());
        article.setUpdatedDateTime(LocalDateTime.now());
        Integer studentId = BaseContext.getCurrentStudentId();

        articleMapper.insert(article);
        stringRedisTemplate.opsForHash().put("cache:articles",
                String.valueOf(article.getId()), JSONUtil.toJsonStr(article));

        userMapper.addArticleCountById(studentId);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
        stringRedisTemplate.delete(CACHE_COUNT_ARTICLES);
    }

    @Override
    public List<ArticleVO> getAll() {
        List<ArticleVO> list = redisUtil.getAllWithHashCache(CACHE_ARTICLES, this::count,
                this.query()::list, Article.class, ArticleVO.class);
        return list.stream().map(vo -> {
            vo.setName(userMapper.selectNameByStudentId(vo.getStudentId()));
            return vo;
        }).sorted((o1, o2) ->
                o2.getUpdatedDateTime().compareTo(o1.getUpdatedDateTime())).toList();
    }

    @Override
    public void update(Article article) {
        articleMapper.updateById(article);
        stringRedisTemplate.opsForHash().delete(CACHE_ARTICLES, String.valueOf(article.getId()));
    }

    @Override
    public void delete(Long id) {
        Object a = stringRedisTemplate.opsForHash().get(CACHE_ARTICLES, String.valueOf(id));
        if (a == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        Article article = JSONUtil.toBean((String) a, Article.class);
        Integer studentId = BaseContext.getCurrentStudentId();

        if (!article.getStudentId().equals(studentId)) {
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);
        }

        articleMapper.deleteById(id);
        stringRedisTemplate.opsForHash().delete(CACHE_ARTICLES, String.valueOf(id));
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
        userMapper.decreaceArticleCount(studentId);
        stringRedisTemplate.delete(CACHE_COUNT_ARTICLES);
    }


}