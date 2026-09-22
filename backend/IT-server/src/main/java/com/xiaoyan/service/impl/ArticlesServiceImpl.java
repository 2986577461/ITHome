package com.xiaoyan.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
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
import java.util.concurrent.TimeUnit;
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
    public static final int MAX_CACHE_SIZE = 50;
    private static final long CACHE_TTL_HOURS = 2;

    private final UsersService usersService;
    private ArticleMapper articleMapper;
    private RedisUtil redisUtil;
    private CommonService commonService;

    /**
     * 从正文 HTML 里抠出图片的 objectName。
     *
     * <p>两种形态都要认：正常是 OSS 的裸 URL，OSS 不可用时降级成了后端的
     * {@code /user/common/local/{objectName}}。只认前者的话，降级期间插进正文的图
     * 在删文章 / 删图时不会被回收，student_file 记录和本地文件都会漏掉。</p>
     *
     * <p>结尾排除 {@code ?} 是为了别把 {@code ?download=1} 带进 objectName——
     * 正文里存的是不带参数的 file_url，但正文是富文本，防一手。</p>
     */
    public static final Pattern IMAGE_PATTERN = Pattern.compile(
            "(?:https?://[^/]+\\.aliyuncs\\.com/|/user/common/local/)([^\"'\\s?]+)");

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
     * field = {@code type:size:page}，value = 那一页的 ArticleVO JSON。</p>
     */
    @Override
    public List<ArticleVO> getPage(@NonNull Integer page, @NonNull @Min(0) Integer type, @NonNull Integer size) {
        if (page < 1 || size < 1) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        int start = (page - 1) * size;
        // 缓存只覆盖每个榜单的前 MAX_CACHE_SIZE 条
        boolean cacheable = page * size - 1 < MAX_CACHE_SIZE;
        String field = cacheType(type) + ":" + size + ":" + page;

        if (cacheable) {
            String cached = redisUtil.getHashField(CACHE_ARTICLE_PAGES, field);
            if (cached != null) {
                // 空数组也是有效值：说明这一页确实没有文章，不用再查库
                return JSONUtil.toList(cached, ArticleVO.class);
            }
        }

        List<ArticleVO> result = queryPageFromDB(start, type, size);

        // 缓存这一页。不加锁也不需要加：整页一次性 HSET，value 是完整的一页，
        // 重复写同一个值没有副作用，并发未命中最多是几个线程各查一次库、写进同一份结果。
        if (cacheable) {
            redisUtil.putHashField(CACHE_ARTICLE_PAGES, field, JSONUtil.toJsonStr(result),
                    CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
        return result;
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

        // 先算出差集，交给 commonService.delete——它把删文件那步挂在本事务的
        // afterCommit 上，所以这里同步调用不会在回滚时把图先删掉
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

        // 文章内容里的图片 objectName 先算出来。真正的删除由 commonService.delete 挂到
        // 本事务的 afterCommit 上执行，否则事务回滚时图片已经删掉，正文里却还引用着它们
        Set<String> objectNames = extractObjectNames(article.getContent());

        if (articleMapper.deleteById(id) == 1) {
            redisUtil.evict(CACHE_ARTICLE_PAGES);
            if (!objectNames.isEmpty()) {
                commonService.delete(objectNames.toArray(String[]::new));
            }
        }
    }

    @Transactional
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