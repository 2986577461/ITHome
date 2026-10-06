package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ArticleCommentMapper;
import com.xiaoyan.mapper.ArticleLikeMapper;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.mapper.CommentLikeMapper;
import com.xiaoyan.pojo.ArticleComment;
import com.xiaoyan.pojo.ArticleLike;
import com.xiaoyan.pojo.CommentLike;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.LikesService;
import com.xiaoyan.vo.ArticleEngagementVO;
import com.xiaoyan.vo.IdCountVO;
import com.xiaoyan.vo.LikeToggleVO;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
public class LikesServiceImpl implements LikesService {

    private static final int MAX_SUMMARY_IDS = 50;

    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleCommentMapper articleCommentMapper;
    private final CommentLikeMapper commentLikeMapper;

    @Override
    @Transactional
    public LikeToggleVO toggleArticle(Long articleId) {
        String studentId = requireLogin();
        ensureArticle(articleId);

        int removed = articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getStudentId, studentId));
        boolean liked;
        if (removed > 0) {
            liked = false;
        } else {
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setStudentId(studentId);
            like.setCreateDateTime(LocalDateTime.now());
            try {
                articleLikeMapper.insert(like);
                liked = true;
            } catch (DuplicateKeyException ignored) {
                liked = true;
            }
        }
        long count = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId));
        return toggleResult(liked, count);
    }

    @Override
    @Transactional
    public LikeToggleVO toggleComment(Long commentId) {
        String studentId = requireLogin();
        ensureComment(commentId);

        int removed = commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getStudentId, studentId));
        boolean liked;
        if (removed > 0) {
            liked = false;
        } else {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setStudentId(studentId);
            like.setCreateDateTime(LocalDateTime.now());
            try {
                commentLikeMapper.insert(like);
                liked = true;
            } catch (DuplicateKeyException ignored) {
                liked = true;
            }
        }
        long count = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId));
        return toggleResult(liked, count);
    }

    @Override
    public List<ArticleEngagementVO> summary(String articleIds) {
        List<Long> ids = parseIds(articleIds);
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> likeCounts = toCountMap(articleLikeMapper.countByArticleIds(ids));
        Map<Long, Long> commentCounts = toCountMap(articleCommentMapper.countTopLevelByArticleIds(ids));
        Set<Long> likedIds = new HashSet<>();
        String studentId = BaseContext.getCurrentStudentId();
        if (studentId != null && !studentId.isBlank()) {
            likedIds.addAll(articleLikeMapper.selectLikedArticleIds(studentId, ids));
        }

        List<ArticleEngagementVO> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            ArticleEngagementVO vo = new ArticleEngagementVO();
            vo.setArticleId(id);
            vo.setLikeCount(likeCounts.getOrDefault(id, 0L));
            vo.setCommentCount(commentCounts.getOrDefault(id, 0L));
            vo.setLiked(likedIds.contains(id));
            result.add(vo);
        }
        return result;
    }

    @Override
    public void deleteByArticle(Long articleId) {
        if (articleId == null) {
            return;
        }
        articleLikeMapper.deleteByArticleId(articleId);
    }

    private LikeToggleVO toggleResult(boolean liked, long count) {
        LikeToggleVO vo = new LikeToggleVO();
        vo.setLiked(liked);
        vo.setLikeCount(count);
        return vo;
    }

    private void ensureArticle(Long articleId) {
        if (articleId == null || articleMapper.selectById(articleId) == null) {
            throw new ParameterException(MessageConstant.ARTICLE_NOT_FOUND);
        }
    }

    private void ensureComment(Long commentId) {
        if (commentId == null) {
            throw new ParameterException(MessageConstant.COMMENT_NOT_FOUND);
        }
        ArticleComment comment = articleCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new ParameterException(MessageConstant.COMMENT_NOT_FOUND);
        }
    }

    private String requireLogin() {
        String studentId = BaseContext.getCurrentStudentId();
        if (studentId == null || studentId.isBlank()) {
            throw new ParameterException(Result.UNAUTHORIZED, MessageConstant.USER_NOT_LOGIN);
        }
        return studentId;
    }

    private List<Long> parseIds(String articleIds) {
        if (articleIds == null || articleIds.isBlank()) {
            return List.of();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (String part : articleIds.split(",")) {
            String text = part.trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                unique.add(Long.valueOf(text));
            } catch (NumberFormatException e) {
                throw new ParameterException(MessageConstant.PARAMETER_ERROR);
            }
        }
        if (unique.size() > MAX_SUMMARY_IDS) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        return new ArrayList<>(unique);
    }

    private Map<Long, Long> toCountMap(List<IdCountVO> rows) {
        Map<Long, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (IdCountVO row : rows) {
            if (row.getId() != null) {
                map.put(row.getId(), row.getTotal() == null ? 0L : row.getTotal());
            }
        }
        return map;
    }
}
