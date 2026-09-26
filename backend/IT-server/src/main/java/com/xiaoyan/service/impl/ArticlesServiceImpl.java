package com.xiaoyan.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.enumeration.ArticleType;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.ArticleImageVO;
import com.xiaoyan.vo.ArticleVO;
import com.xiaoyan.vo.MyArticleVO;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLE_PAGES;

@Service
@AllArgsConstructor
@Slf4j
public class ArticlesServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticlesService {

    /**
     * 分页缓存最多覆盖的文章数。
     *
     * <p>超出这个位置的分页直接查库：否则客户端用一个很大的 page 就能无限往缓存里塞条目，
     * field 数量没有上界。</p>
     */

    private final UsersService usersService;
    private ArticleMapper articleMapper;
    private RedisUtil redisUtil;
    private CommonService commonService;

    public static final Pattern IMAGE_PATTERN = Pattern.compile("https?://[^/]+\\.aliyuncs\\.com/([^\"'\\s]+)");

    @Override
    public Long getCount(Integer type) {
        LambdaQueryWrapper<Article> lqw = new LambdaQueryWrapper<>();
        if (type != null && type != ArticleType.ALL.getCode()) {
            lqw.eq(Article::getType, type);
        }
        return this.count(lqw);
    }

    @Override
    @Transactional
    public void upload(ArticleDTO articleDTO) {
        Article article = BeanUtil.toBean(articleDTO, Article.class);
        String studentId = BaseContext.getCurrentStudentId();
        article.setStudentId(studentId);

        LocalDateTime now = LocalDateTime.now();
        article.setReleaseDateTime(now);
        article.setUpdatedDateTime(now);

        articleMapper.insert(article);

        redisUtil.evict(CACHE_ARTICLE_PAGES);
    }

    @Override
    public List<MyArticleVO> getMyPage(@NonNull Integer page, @NonNull Integer size) {
        if (page < 1 || size < 1) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        int start = (page - 1) * size;
        return articleMapper.selectMyPage(
                start, BaseContext.getCurrentStudentId(), size);
    }

    /**
     * 分页查询文章。type 为 null 或 0 表示不按类型过滤。
     *
     * <p>缓存的是「已经切好页的一个数组」，整个榜单的全部分页装在同一个 Hash 里：
     * field = {@code type:size:page}，value = 那一页的 ArticleVO JSON。
     * 缓存未命中时按 field 加互斥锁，只有持锁线程回源数据库，其余线程拿锁后会再次检查缓存。</p>
     */
    @Override
    public List<ArticleVO> getPage(Integer page, Integer type, Integer size) {
        if (page < 1 || size < 1) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        int start = (page - 1) * size;
        String field = cacheType(type) + ":" + size + ":" + page;

        return redisUtil.queryHashWithMutex(
                CACHE_ARTICLE_PAGES,
                field,
                ArticleVO.class,
                ignored -> queryPageFromDB(start, type, size));
    }

    /**
     * 缓存 field 里统一用 ArticleType.ALL 的 code 代表「全部」
     */
    private int cacheType(Integer type) {
        return type == null ? ArticleType.ALL.getCode() : type;
    }

    private List<ArticleVO> queryPageFromDB(int start, Integer type, int size) {
        Integer databaseType = type == null || type == ArticleType.ALL.getCode() ? null : type;
        return articleMapper.selectPage(start, databaseType, size);
    }


    @Override
    @Transactional
    public void update(ArticleDTO articleDTO) {
        Article oldArticle = this.getById(articleDTO.getId());
        if (oldArticle == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        usersService.checkOwnerOrAdmin(oldArticle.getStudentId());

        // 先计算需要删除的文件，等数据库事务提交后再删除
        Set<String> oldObjectNames = extractObjectNames(oldArticle.getContent());
        Set<String> newObjectNames = extractObjectNames(articleDTO.getContent());
        List<String> toDelete = oldObjectNames.stream()
                .filter(name -> !newObjectNames.contains(name))
                .toList();

        // 更新文章
        Article article = BeanUtil.toBean(articleDTO, Article.class);
        article.setUpdatedDateTime(LocalDateTime.now());
        if (articleMapper.updateById(article) == 1) {
            redisUtil.evict(CACHE_ARTICLE_PAGES);
            if (!toDelete.isEmpty()) {
                commonService.delete(toDelete.toArray(String[]::new));
            }
        }
    }

    private Set<String> extractObjectNames(String content) {
        if (content == null || content.isEmpty()) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        Matcher matcher = IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    @Override
    public Integer getArticlePosition(Long articleId) {

        Article article = this.getById(articleId);
        if (article == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        return articleMapper.countBefore(article.getUpdatedDateTime());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Article article = this.getById(id);
        if (article == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        usersService.checkOwnerOrAdmin(article.getStudentId());

        // 文章内容里的图片 objectName 先算出来，事务提交后再删 OSS 文件，
        // 否则事务回滚时图片已经删掉，数据库里却还留着对它的引用
        Set<String> objectNames = extractObjectNames(article.getContent());

        if (articleMapper.deleteById(id) == 1) {
            redisUtil.evict(CACHE_ARTICLE_PAGES);
            if (!objectNames.isEmpty()) {
                commonService.delete(objectNames.toArray(String[]::new));
            }
        }
    }

    public List<ArticleImageVO> batchUploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        List<ArticleImageVO> results = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                StudentFile sf = commonService.upload(file);
                results.add(new ArticleImageVO(sf.getId(), sf.getFileUrl()));
            }
        } catch (IOException e) {
            throw new ParameterException(MessageConstant.ALIOSS_NETWORK_ERROR);
        }
        return results;
    }

}