package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.vo.ArticleImageVO;
import com.xiaoyan.vo.ArticleVO;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface ArticlesService extends IService<Article> {

    Long getCount(Integer type);

    void upload(ArticleDTO articleDTO);

    List<ArticleVO> getPage(@NonNull Integer page, Integer type, @NonNull Integer size);

    void delete(Long id);

    void update(ArticleDTO article);

    List<ArticleVO> getMyArticles(@NonNull Integer page, @NonNull Integer size, Integer studentId);

    /**
     * 批量删除图片（删OSS文件 + student_file记录 + article_image关联记录）
     */
    void deleteBatch(List<Long> studentFileIds);

    /**
     * 批量上传 MultipartFile 到 OSS，返回 student_file_id + file_url 列表（保持输入顺序）
     */
    List<ArticleImageVO> batchUploadFiles(List<MultipartFile> files);

    Integer getArticlePosition(Long articleId);
}