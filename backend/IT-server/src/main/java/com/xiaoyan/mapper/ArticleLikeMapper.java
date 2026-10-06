package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.ArticleLike;
import com.xiaoyan.vo.IdCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    List<IdCountVO> countByArticleIds(@Param("ids") List<Long> ids);

    List<Long> selectLikedArticleIds(@Param("studentId") String studentId, @Param("ids") List<Long> ids);

    int deleteByArticleId(@Param("articleId") Long articleId);
}
