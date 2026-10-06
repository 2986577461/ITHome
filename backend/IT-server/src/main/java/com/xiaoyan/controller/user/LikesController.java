package com.xiaoyan.controller.user;

import com.xiaoyan.dto.ArticleLikeDTO;
import com.xiaoyan.dto.CommentLikeDTO;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.LikesService;
import com.xiaoyan.vo.ArticleEngagementVO;
import com.xiaoyan.vo.LikeToggleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userLikes")
@RequestMapping("user/likes")
@AllArgsConstructor
@Validated
@Tag(name = "点赞")
public class LikesController {

    private final LikesService likesService;

    @PostMapping("toggle")
    @Operation(summary = "点赞或取消点赞文章")
    public Result<LikeToggleVO> toggle(@RequestBody @Valid ArticleLikeDTO dto) {
        return Result.success(likesService.toggleArticle(dto.getArticleId()));
    }

    @PostMapping("comments/toggle")
    @Operation(summary = "点赞或取消点赞评论")
    public Result<LikeToggleVO> toggleComment(@RequestBody @Valid CommentLikeDTO dto) {
        return Result.success(likesService.toggleComment(dto.getCommentId()));
    }

    @GetMapping("summary")
    @Operation(summary = "批量查询文章点赞和评论数")
    public Result<List<ArticleEngagementVO>> summary(@RequestParam(required = false) String articleIds) {
        return Result.success(likesService.summary(articleIds));
    }
}
