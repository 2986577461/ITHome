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
import com.xiaoyan.service.PermissionService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.vo.ArticleImageVO;
import com.xiaoyan.vo.ArticleVO;
import com.xiaoyan.vo.StudentVO;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.RANKING_ARTICLES;

@Service
@AllArgsConstructor
public class ArticlesServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticlesService {

    /**
     * ZSET缓存最多保留的文章数
     */
    public static final int MAX_CACHE_SIZE = 50;
    private final UsersService usersService;
    private final PermissionService permissionService;
    private ArticleMapper articleMapper;
    private StringRedisTemplate stringRedisTemplate;
    private CommonService commonService;

    public static final Pattern IMAGE_PATTERN = Pattern.compile("https?://[^/]+\\.aliyuncs\\.com/([^\"'\\s]+)");

    @Override
    public Long getCount(Integer type) {
        LambdaQueryWrapper<Article> lqw = new LambdaQueryWrapper<>();
        if (type != null && type != ArticleType.ALL.ordinal()) {
            lqw.eq(Article::getType, type);
        }
        return this.count(lqw);
    }

    @Override
    public void upload(ArticleDTO articleDTO) {
        Article article = BeanUtil.toBean(articleDTO, Article.class);
        Integer studentId = BaseContext.getCurrentStudentId();
        article.setStudentId(studentId);

        LocalDateTime now = LocalDateTime.now();
        article.setReleaseDateTime(now);
        article.setUpdatedDateTime(now);

        articleMapper.insert(article);

        // 增量更新ZSET：加入新文章，裁剪到50条（末位淘汰）
        ArticleVO vo = BeanUtil.toBean(article, ArticleVO.class);
        StudentVO user = usersService.getUser(studentId);
        vo.setName(user.getName());
        vo.setAvatar(user.getAvatar());

        cacheArticle(vo, null);
    }

    @Override
    public List<ArticleVO> getMyPage(@NonNull Integer page, @NonNull Integer size) {
        if (page < 1 || size < 1) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        int start = (page - 1) * size;
        List<Article> articles = articleMapper.selectPageByStudentId(
                start, BaseContext.getCurrentStudentId(), size);
        return toArticleVOList(articles);
    }

    /**
     * 分页查询文章。
     *
     * <pre>
     * studentId != null → 查自己的文章
     * studentId == null → 查全部（可按type过滤）
     * </pre>
     */
    @Override
    public List<ArticleVO> getPage(@NonNull Integer page, @NonNull @Min(0) Integer type, @NonNull Integer size) {
        if (page < 1 || size < 1) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        int start = (page - 1) * size;
        int end = page * size - 1;

        List<ArticleVO> result;

        if (end < MAX_CACHE_SIZE) {
            result = getPageFromCache(start, end, type);
            if (result != null && result.size() == size) {
                return result;
            }

            synchronized (this) {
//                锁内复查缓存
                result = getPageFromCache(start, end, type);
                if (result != null && result.size() == size) {
                    return result;
                }
                buildLatestCache();
            }
            result = getPageFromCache(start, end, type);
            if (result != null && result.size() == size) {
                return result;
            }

        }
        // 不在缓存范围内 / 缓存不够 → 数据库兜底
        result = queryPageFromDB(start, type, size);
        return result;
    }

    private List<ArticleVO> getPageFromCache(int start, int end, Integer type) {
        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        String rankingKey = rankingKey(type);
        Long cacheSize = ops.size(rankingKey);

        if (cacheSize == null || cacheSize <= end) {
            return null;
        }

        Set<String> idSet = ops.reverseRange(rankingKey, start, end);
        if (idSet == null || idSet.isEmpty()) {
            return List.of();
        }

        List<Object> cachedArticles = stringRedisTemplate.opsForHash()
                .multiGet(CACHE_ARTICLES, new ArrayList<>(idSet));

        return cachedArticles.stream()
                .filter(Objects::nonNull)
                .map(value -> JSONUtil.toBean((String) value, ArticleVO.class))
                .toList();
    }

    public void buildLatestCache() {
        // 先清空详情和所有已知排名索引
        stringRedisTemplate.delete(CACHE_ARTICLES);
        for (ArticleType articleType : ArticleType.values()) {
            stringRedisTemplate.delete(rankingKey(articleType.ordinal()));
        }

        // 每个类型取最新50条
        List<ArticleVO> window = articleMapper.selectWindow(MAX_CACHE_SIZE);

        Map<Integer, List<ArticleVO>> map = new HashMap<>();
        for (ArticleVO vo : window) {
            stringRedisTemplate.opsForHash().put(CACHE_ARTICLES, String.valueOf(vo.getId()),
                    JSONUtil.toJsonStr(vo));
            map.computeIfAbsent(vo.getType(), key -> new ArrayList<>())
                    .add(vo);
        }

        // 全部类型只缓存全局最新50条
        List<ArticleVO> latest = window.stream()
                .sorted(Comparator.comparing(ArticleVO::getUpdatedDateTime, Comparator.reverseOrder())
                        .thenComparing(ArticleVO::getId, Comparator.reverseOrder()))
                .limit(MAX_CACHE_SIZE)
                .toList();
        addToRanking(ArticleType.ALL.ordinal(), latest);

        map.forEach(this::addToRanking);
    }


    private List<ArticleVO> queryPageFromDB(int start, Integer type, int size) {
        Integer databaseType = type == null || type == ArticleType.ALL.ordinal() ? null : type;
        return articleMapper.selectPage(start, databaseType, size);
    }

    private String rankingKey(Integer type) {
        int rankingType = type == null ? ArticleType.ALL.ordinal() : type;
        return RANKING_ARTICLES + ":" + rankingType;
    }

    private void addToRanking(Integer type, List<ArticleVO> articles) {
        if (articles.isEmpty()) {
            return;
        }

        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        articles.forEach(vo -> tuples.add(
                new DefaultTypedTuple<>(String.valueOf(vo.getId()), vo.getScore())));
        stringRedisTemplate.opsForZSet().add(rankingKey(type), tuples);
    }

    private void cacheArticle(ArticleVO vo, Integer oldType) {
        String articleId = String.valueOf(vo.getId());
        if (oldType != null && !Objects.equals(oldType, vo.getType())) {
            stringRedisTemplate.opsForZSet().remove(rankingKey(oldType), articleId);
        }

        stringRedisTemplate.opsForHash().put(CACHE_ARTICLES, articleId, JSONUtil.toJsonStr(vo));
        stringRedisTemplate.opsForZSet().add(rankingKey(vo.getType()), articleId, vo.getScore());
        stringRedisTemplate.opsForZSet().add(rankingKey(ArticleType.ALL.ordinal()), articleId, vo.getScore());
        trimRanking(rankingKey(vo.getType()));
        trimRanking(rankingKey(ArticleType.ALL.ordinal()));
    }

    private void trimRanking(String key) {
        Long cacheSize = stringRedisTemplate.opsForZSet().size(key);
        if (cacheSize != null && cacheSize > MAX_CACHE_SIZE) {
            stringRedisTemplate.opsForZSet().removeRange(key, 0,
                    cacheSize - MAX_CACHE_SIZE - 1);
        }
    }

    private List<ArticleVO> toArticleVOList(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        Set<Integer> studentIds = new HashSet<>();
        List<ArticleVO> vos = articles.stream().map(r -> {
            studentIds.add(r.getStudentId());
            return BeanUtil.toBean(r, ArticleVO.class);
        }).toList();

        // 批量查姓名
        if (!studentIds.isEmpty()) {
            Map<Integer, String> nameMap = new HashMap<>();
            Map<Integer, String> avatarMap = new HashMap<>();

            List<StudentVO> students = usersService.getAll().stream()
                    .filter(vo -> studentIds.contains(vo.getStudentId())).toList();

            students.forEach(vo -> {
                nameMap.put(vo.getStudentId(), vo.getName());
                avatarMap.put(vo.getStudentId(), vo.getAvatar());
            });
            vos.forEach(vo -> {
                vo.setName(nameMap.get(vo.getStudentId()));
                vo.setAvatar(avatarMap.get(vo.getStudentId()));
            });
        }

        return vos;
    }

    @Override
    @Transactional
    public void update(ArticleDTO articleDTO) {
        Article oldArticle = this.getById(articleDTO.getId());
        if (oldArticle == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        permissionService.checkOwnerOrAdminPermission(oldArticle.getStudentId());

        // 删掉旧内容中不再引用的 OSS 文件
        Set<String> oldObjectNames = extractObjectNames(oldArticle.getContent());
        Set<String> newObjectNames = extractObjectNames(articleDTO.getContent());
        List<String> toDelete = oldObjectNames.stream()
                .filter(name -> !newObjectNames.contains(name))
                .toList();
        if (!toDelete.isEmpty()) {
            commonService.delete(toDelete.toArray(String[]::new));
        }

        // 更新文章
        Article article = BeanUtil.toBean(articleDTO, Article.class);
        article.setUpdatedDateTime(LocalDateTime.now());
        articleMapper.updateById(article);

        ArticleVO vo = BeanUtil.toBean(article, ArticleVO.class);
        vo.setStudentId(oldArticle.getStudentId());
        StudentVO user = usersService.getUser(oldArticle.getStudentId());
        vo.setName(user.getName());
        vo.setAvatar(user.getAvatar());
        cacheArticle(vo, oldArticle.getType());
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
    public void delete(Long id) {
        Article article = this.getById(id);
        if (article == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        permissionService.checkOwnerOrAdminPermission(article.getStudentId());

        // 提取文章内容中的所有图片 objectName 并删除 OSS 文件
        deleteImages(article.getContent());

        articleMapper.deleteById(id);
        // 从ZSET中精确移除该条
        removeFromCache(id, article.getType());
    }


    private void deleteImages(String content) {
        Set<String> objectNames = extractObjectNames(content);
        if (!objectNames.isEmpty()) {
            commonService.delete(objectNames.toArray(String[]::new));
        }
    }


    private void removeFromCache(Long id, Integer type) {
        if (id != null && type != null && type >= 0) {
            String sId = String.valueOf(id);
            stringRedisTemplate.opsForZSet().remove(RANKING_ARTICLES + ":" + type, sId);
            stringRedisTemplate.opsForZSet().remove(RANKING_ARTICLES + ":" + ArticleType.ALL.ordinal(), sId);
            stringRedisTemplate.opsForHash().delete(CACHE_ARTICLES, sId);
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
            throw new RuntimeException(MessageConstant.ALIOSS_NETWORK_ERROR);
        }
        return results;
    }

}