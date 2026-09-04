package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.vo.ArticleVO;
import com.xiaoyan.vo.MyArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    List<ArticleVO> selectPage(Integer offset, Integer type, Integer size);

    List<ArticleVO> selectWindow(Integer size);

    List<Article> selectPageByStudentId(Integer offset, Integer studentId, Integer size);

    List<MyArticleVO> selectMyPage(Integer offset, Integer studentId, Integer size);

    List<Article> selectByStudentIds(@Param("studentIds") List<Integer> studentIds);

    int deleteByStudentIds(@Param("studentIds") List<Integer> studentIds);

    /** 统计排在指定文章前面的文章数（updated_date_time DESC, id DESC） */
    int countBefore(LocalDateTime updatedDateTime);

    int selectCountByStudentId(Integer studentId);

    void deleteByStudentId(Integer studentId);
}
