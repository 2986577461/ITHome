package com.xiaoyan.controller.user;

import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.vo.MyArticleVO;
import com.xiaoyan.vo.ArticleImageVO;
import com.xiaoyan.vo.ArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController("userArticles")
@RequestMapping("user/articles")
@Tag(name = "文章管理")
@Slf4j
public class ArticlesController {

    @Resource
    private ArticlesService articlesService;

    @GetMapping("page")
    @Operation(summary = "分页查询文章")
    public Result<List<ArticleVO>> getPage(@NonNull @Min(1) Integer page, Integer type, @NonNull Integer size) {
        return Result.success(articlesService.getPage(page, type, size));
    }

    @GetMapping("my-page")
    @Operation
    public Result<List<MyArticleVO>> getMyPage(@NonNull @Min(1) Integer page, @NonNull Integer size) {
        return Result.success(articlesService.getMyPage(page, size));
    }


    @GetMapping("count")
    @Operation(summary = "获取文章总数")
    public Result<Long> getCount(Integer type) {
        log.info("获取文章总数");
        return Result.success(articlesService.getCount(type));
    }

    @GetMapping("position")
    @Operation(summary = "返回文章所在位置")
    public Result<Integer> getArticlePosition(long id) {
        log.info("获取文章{}所在页数", id);
        return Result.success(articlesService.getArticlePosition(id));
    }

    @PostMapping
    @Operation(summary = "上传文章")
    public Result<String> upload(@RequestBody @Valid ArticleDTO articleDTO) {
        log.info("上传文章：{}", articleDTO);
        articlesService.upload(articleDTO);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改文章")
    public Result<String> update(@RequestBody ArticleDTO articleDTO) {
        log.info("修改文章:{}", articleDTO);
        articlesService.update(articleDTO);
        return Result.success();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除文章")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除文章:{}", id);
        articlesService.delete(id);
        return Result.success();
    }

    @PostMapping("image")
    @Operation(summary = "批量上传图片（FormData）")
    public Result<List<ArticleImageVO>> batchUpload(@RequestParam("files") List<MultipartFile> files) {
        log.info("批量上传 {} 张图片", files.size());
        List<ArticleImageVO> results = articlesService.batchUploadFiles(files);
        return Result.success(results);
    }
}
