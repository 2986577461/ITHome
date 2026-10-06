package com.xiaoyan.service.impl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.CommentDTO;
import com.xiaoyan.dto.CommentReplyDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ArticleCommentMapper;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.mapper.CommentLikeMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.ArticleComment;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommentsService;
import com.xiaoyan.vo.CommentPageVO;
import com.xiaoyan.vo.CommentVO;
import com.xiaoyan.vo.IdCountVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
public class CommentsServiceImpl implements CommentsService {

    public static final int PAGE_SIZE = 5;

    private final ArticleMapper articleMapper;
    private final ArticleCommentMapper articleCommentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final StudentFileMapper studentFileMapper;

    @Override
    public CommentPageVO page(Long articleId, Integer page) {
        ensureArticle(articleId);
        int pageNo = page == null ? 1 : page;
        if (pageNo < 1) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        int offset = (pageNo - 1) * PAGE_SIZE;
        long total = articleCommentMapper.countTopLevel(articleId);
        List<CommentVO> records = articleCommentMapper.selectTopLevel(articleId, offset, PAGE_SIZE);
        if (records == null) {
            records = new ArrayList<>();
        }
        fillRepliesAndLikes(records);

        CommentPageVO vo = new CommentPageVO();
        vo.setRecords(records);
        vo.setTotal(total);
        vo.setHasMore((long) offset + records.size() < total);
        return vo;
    }

    @Override
    @Transactional
    public CommentVO add(CommentDTO dto) {
        String studentId = requireLogin();
        ensureArticle(dto.getArticleId());
        ArticleComment comment = new ArticleComment();
        comment.setArticleId(dto.getArticleId());
        comment.setStudentId(studentId);
        comment.setContent(dto.getContent().trim());
        comment.setCreateDateTime(LocalDateTime.now());
        articleCommentMapper.insert(comment);
        return toNewVo(comment, studentId);
    }

    @Override
    @Transactional
    public CommentVO reply(CommentReplyDTO dto) {
        String studentId = requireLogin();
        ArticleComment parent = articleCommentMapper.selectById(dto.getCommentId());
        if (parent == null) {
            throw new ParameterException(MessageConstant.COMMENT_NOT_FOUND);
        }
        if (parent.getParentId() != null) {
            throw new ParameterException(MessageConstant.REPLY_TARGET_INVALID);
        }
        ArticleComment comment = new ArticleComment();
        comment.setArticleId(parent.getArticleId());
        comment.setParentId(parent.getId());
        comment.setStudentId(studentId);
        comment.setContent(dto.getContent().trim());
        comment.setCreateDateTime(LocalDateTime.now());
        articleCommentMapper.insert(comment);
        return toNewVo(comment, studentId);
    }

    @Override
    public List<CommentVO> replies(Long commentId) {
        ArticleComment parent = articleCommentMapper.selectById(commentId);
        if (parent == null || parent.getParentId() != null) {
            throw new ParameterException(MessageConstant.COMMENT_NOT_FOUND);
        }
        List<CommentVO> replies = articleCommentMapper.selectReplies(List.of(commentId));
        fillLikes(replies);
        return replies;
    }

    @Override
    public void deleteByArticle(Long articleId) {
        if (articleId == null) {
            return;
        }
        commentLikeMapper.deleteByArticleId(articleId);
        articleCommentMapper.deleteByArticleId(articleId);
    }

    private void fillRepliesAndLikes(List<CommentVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> parentIds = records.stream().map(CommentVO::getId).toList();
        List<CommentVO> replies = articleCommentMapper.selectReplies(parentIds);
        if (replies == null) {
            replies = List.of();
        }
        Map<Long, List<CommentVO>> byParent = new HashMap<>();
        for (CommentVO reply : replies) {
            byParent.computeIfAbsent(reply.getParentId(), key -> new ArrayList<>()).add(reply);
        }
        List<CommentVO> all = new ArrayList<>(records);
        all.addAll(replies);
        fillLikes(all);
        for (CommentVO comment : records) {
            List<CommentVO> children = byParent.get(comment.getId());
            comment.setReplies(children == null ? new ArrayList<>() : children);
            normalize(comment);
        }
    }

    private void fillLikes(List<CommentVO> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        List<Long> ids = comments.stream().map(CommentVO::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (IdCountVO row : commentLikeMapper.countByCommentIds(ids)) {
            if (row.getId() != null) {
                counts.put(row.getId(), row.getTotal() == null ? 0L : row.getTotal());
            }
        }
        Set<Long> liked = new HashSet<>();
        String studentId = BaseContext.getCurrentStudentId();
        if (studentId != null && !studentId.isBlank()) {
            liked.addAll(commentLikeMapper.selectLikedCommentIds(studentId, ids));
        }
        for (CommentVO comment : comments) {
            comment.setLikeCount(counts.getOrDefault(comment.getId(), 0L));
            comment.setLiked(liked.contains(comment.getId()));
            normalize(comment);
        }
    }

    private CommentVO toNewVo(ArticleComment comment, String studentId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setParentId(comment.getParentId());
        vo.setStudentId(studentId);
        vo.setContent(comment.getContent());
        vo.setCreateDateTime(comment.getCreateDateTime());
        vo.setLikeCount(0L);
        vo.setLiked(false);
        vo.setReplies(new ArrayList<>());
        Student student = userMapper.selectByStudentId(studentId);
        if (student == null || student.getName() == null || student.getName().isBlank()) {
            vo.setName("匿名");
        } else {
            vo.setName(student.getName());
            if (student.getAvatarId() != null && student.getAvatarId() > 0) {
                StudentFile file = studentFileMapper.selectById(student.getAvatarId());
                if (file != null) {
                    vo.setAvatar(file.getFileUrl());
                }
            }
        }
        return vo;
    }

    private void normalize(CommentVO comment) {
        if (comment.getName() == null || comment.getName().isBlank()) {
            comment.setName("匿名");
        }
        if (comment.getReplies() == null) {
            comment.setReplies(new ArrayList<>());
        }
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0L);
        }
        if (comment.getLiked() == null) {
            comment.setLiked(false);
        }
    }

    private void ensureArticle(Long articleId) {
        if (articleId == null || articleMapper.selectById(articleId) == null) {
            throw new ParameterException(MessageConstant.ARTICLE_NOT_FOUND);
        }
    }

    private String requireLogin() {
        String studentId = BaseContext.getCurrentStudentId();
        if (studentId == null || studentId.isBlank()) {
            throw new ParameterException(Result.UNAUTHORIZED, MessageConstant.USER_NOT_LOGIN);
        }
        return studentId;
    }
}
