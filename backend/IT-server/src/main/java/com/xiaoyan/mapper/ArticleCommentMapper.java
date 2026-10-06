package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.ArticleComment;
import com.xiaoyan.vo.CommentVO;
import com.xiaoyan.vo.IdCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

    List<CommentVO> selectTopLevel(@Param("articleId") Long articleId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    long countTopLevel(@Param("articleId") Long articleId);

    List<IdCountVO> countTopLevelByArticleIds(@Param("ids") List<Long> ids);

    List<CommentVO> selectReplies(@Param("parentIds") List<Long> parentIds);

    int deleteByArticleId(@Param("articleId") Long articleId);
}
