package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.Article;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    List<Article> selectPage(Integer offset, Integer type, Integer size);

    List<Article> selectPageByStudentId(Integer offset, Integer studentId, Integer size);

    /** 统计排在指定文章前面的文章数（updated_date_time DESC, id DESC） */
    int countBefore(LocalDateTime updatedDateTime);
}