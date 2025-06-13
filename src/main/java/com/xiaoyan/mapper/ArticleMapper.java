package com.xiaoyan.mapper;

import com.xiaoyan.pojo.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleMapper{

    void insert(Article article);

    @Select("SELECT COUNT(1) FROM article")
    Integer getCount();

    @Select("SELECT * FROM article")
    List<Article> selectAll();

    void updateById(Article article);

    @Select("SELECT * FROM article WHERE id=#{id}")
    Article selectById(Long id);

    @Delete("DELETE FROM article WHERE id=#{id}")
    void deleteById(Long id);
}
