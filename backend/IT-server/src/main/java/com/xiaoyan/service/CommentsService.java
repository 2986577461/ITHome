package com.xiaoyan.service;

import com.xiaoyan.dto.CommentDTO;
import com.xiaoyan.dto.CommentReplyDTO;
import com.xiaoyan.vo.CommentPageVO;
import com.xiaoyan.vo.CommentVO;

import java.util.List;

public interface CommentsService {

    /** 顶层评论从旧到新，固定每页 5 条。 */
    CommentPageVO page(Long articleId, Integer page);

    CommentVO add(CommentDTO dto);

    CommentVO reply(CommentReplyDTO dto);

    List<CommentVO> replies(Long commentId);

    void deleteByArticle(Long articleId);
}
