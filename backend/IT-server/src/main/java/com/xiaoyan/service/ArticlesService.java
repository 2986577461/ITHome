package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.vo.ArticleImageVO;
import com.xiaoyan.vo.ArticleVO;
import jakarta.validation.constraints.Min;
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

    List<ArticleImageVO> batchUploadFiles(List<MultipartFile> files);

    Integer getArticlePosition(Long articleId);

    List<ArticleVO> getMyPage(@NonNull @Min(1) Integer page, @NonNull Integer size);
}