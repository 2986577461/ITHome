package com.xiaoyan.service;

import com.xiaoyan.vo.ArticleEngagementVO;
import com.xiaoyan.vo.LikeToggleVO;

import java.util.List;

public interface LikesService {

    LikeToggleVO toggleArticle(Long articleId);

    LikeToggleVO toggleComment(Long commentId);

    /** articleIds 用英文逗号分隔。未登录时 liked 一律为 false。 */
    List<ArticleEngagementVO> summary(String articleIds);

    void deleteByArticle(Long articleId);
}
