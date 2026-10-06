package com.xiaoyan.controller.user;

import com.xiaoyan.dto.CommentDTO;
import com.xiaoyan.dto.CommentReplyDTO;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommentsService;
import com.xiaoyan.vo.CommentPageVO;
import com.xiaoyan.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userComments")
@RequestMapping("user/comments")
@AllArgsConstructor
@Validated
@Tag(name = "评论")
public class CommentsController {

    private final CommentsService commentsService;

    @GetMapping
    @Operation(summary = "分页查询评论，每页5条，从旧到新")
    public Result<CommentPageVO> page(@RequestParam Long articleId,
                                       @RequestParam(required = false) Integer page) {
        return Result.success(commentsService.page(articleId, page));
    }

    @PostMapping
    @Operation(summary = "发表评论")
    public Result<CommentVO> add(@RequestBody @Valid CommentDTO dto) {
        return Result.success(commentsService.add(dto));
    }

    @PostMapping("reply")
    @Operation(summary = "回复评论")
    public Result<CommentVO> reply(@RequestBody @Valid CommentReplyDTO dto) {
        return Result.success(commentsService.reply(dto));
    }

    @GetMapping("{id}/replies")
    @Operation(summary = "查询某条评论的回复")
    public Result<List<CommentVO>> replies(@PathVariable Long id) {
        return Result.success(commentsService.replies(id));
    }
}
