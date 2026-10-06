package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.CommentLike;
import com.xiaoyan.vo.IdCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    List<IdCountVO> countByCommentIds(@Param("ids") List<Long> ids);

    List<Long> selectLikedCommentIds(@Param("studentId") String studentId, @Param("ids") List<Long> ids);

    int deleteByArticleId(@Param("articleId") Long articleId);
}
