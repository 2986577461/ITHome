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
import com.xiaoyan.vo.MyArticleVO;
import com.xiaoyan.vo.StudentVO;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS_ALL;
import static com.xiaoyan.constant.RedisConstant.RANKING_ARTICLES;

@Service
@AllArgsConstructor
public class ArticlesServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticlesService {

    /**
     * ZSET缓存最多保留的文章数
     */
    public static final int MAX_CACHE_SIZE = 50;
    private static final long CACHE_TTL_HOURS = 2;
    // 缓存构建完成标记：只有标记不存在时才触发重建
    private static final String CACHE_READY = RANKING_ARTICLES + ":ready";
    private static final DefaultRedisScript<Long> SWAP_CACHE_SCRIPT = loadScript(
            "lua/swap-article-cache.lua");

    private final Object cacheRebuildLock = new Object();
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
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);

        // 增量更新ZSET：加入新文章，裁剪到50条（末位淘汰）
        ArticleVO vo = BeanUtil.toBean(article, ArticleVO.class);
        StudentVO user = usersService.getUser(studentId);
        vo.setName(user.getName());
        vo.setAvatar(user.getAvatar());

        cacheArticle(vo, null);
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

        // Redis 只缓存每个榜单的前50条，超过范围直接查询数据库
        if (end < MAX_CACHE_SIZE) {
            result = getPageFromCache(start, end, type);
            if (result != null && result.size() == size) {
                return result;
            }

            // 同一实例内只允许一个线程重建缓存
            synchronized (cacheRebuildLock) {
                // 等待锁期间可能已经有其他线程完成重建，因此需要再次检查
                result = getPageFromCache(start, end, type);
                if (result != null && result.size() == size) {
                    return result;
                }

                // 缓存未构建
                if (!stringRedisTemplate.hasKey(CACHE_READY)) {
                    buildLatestCache();

                    // 重建完成后再尝试读取一次
                    result = getPageFromCache(start, end, type);
                    if (result != null && result.size() == size) {
                        return result;
                    }
                }
            }
        }
        // 不在缓存范围内，或缓存中的文章数量不足一页，查询数据库
        result = queryPageFromDB(start, type, size);
        return result;
    }

    /**
     * 根据类型从 Redis 查询一页文章。
     * ZSET 只保存文章 ID 和排序分数，Hash 保存文章详情。
     */
    private List<ArticleVO> getPageFromCache(int start, int end, Integer type) {
//        if(true){
//            return null;
//        }
        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        String rankingKey = rankingKey(type);
        Long cacheSize = ops.size(rankingKey);

        // end 是下标，cacheSize 是数量；数量不足时不能返回完整一页
        if (cacheSize == null || cacheSize <= end) {
            return null;
        }

        // 取出 ID 和 score，不依赖 Set 的遍历顺序
        Set<ZSetOperations.TypedTuple<String>> tuples = ops.reverseRangeWithScores(rankingKey, start, end);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        // 按数据库相同的规则排序：更新时间倒序，ID 倒序
        List<String> articleIds = tuples.stream()
                .sorted(Comparator.<ZSetOperations.TypedTuple<String>>comparingDouble(tuple ->
                                tuple.getScore() == null ? Double.NEGATIVE_INFINITY : tuple.getScore())
                        .reversed()
                        .thenComparing(Comparator.<ZSetOperations.TypedTuple<String>>comparingLong(
                                tuple -> Long.parseLong(tuple.getValue())).reversed()))
                .map(ZSetOperations.TypedTuple::getValue)
                .toList();

        // HMGET 返回值的顺序与 hashKeys 保持一致
        List<Object> hashKeys = new ArrayList<>(articleIds);
        List<Object> cachedArticles = stringRedisTemplate.opsForHash()
                .multiGet(CACHE_ARTICLES, hashKeys);

        return cachedArticles.stream()
                .filter(Objects::nonNull)
                .map(value -> JSONUtil.toBean((String) value, ArticleVO.class))
                .toList();
    }

    public void buildLatestCache() {
        synchronized (cacheRebuildLock) {
            rebuildLatestCache();
        }
    }

    /**
     * 使用临时 key 构建完整缓存，构建完成后通过 Lua 一次性切换正式 key。
     * 这样读请求不会看到只写了一半的缓存。
     */
    private void rebuildLatestCache() {
        List<ArticleVO> window = articleMapper.selectWindow(MAX_CACHE_SIZE);

        String buildId = UUID.randomUUID().toString();
        String temporaryDetailsKey = CACHE_ARTICLES + ":rebuild:" + buildId;
        List<String> temporaryRankingKeys = new ArrayList<>();
        List<String> temporaryKeys = new ArrayList<>();
        temporaryKeys.add(temporaryDetailsKey);
        for (ArticleType articleType : ArticleType.values()) {
            // 临时排名 key 与正式排名 key 一一对应，最后由 Lua 改名
            String key = rankingKey(articleType.ordinal()) + ":rebuild:" + buildId;
            temporaryRankingKeys.add(key);
            temporaryKeys.add(key);
        }

        try {
            // 一个 Hash 保存详情，多个 ZSET 保存不同类型的排序索引
            Map<Integer, List<ArticleVO>> articlesByType = new HashMap<>();
            for (ArticleVO vo : window) {
                stringRedisTemplate.opsForHash().put(temporaryDetailsKey, String.valueOf(vo.getId()),
                        JSONUtil.toJsonStr(vo));
                articlesByType.computeIfAbsent(vo.getType(), key -> new ArrayList<>())
                        .add(vo);
            }

            // window 是每种类型最新50条，全部榜单还需要取全局最新50条
            List<ArticleVO> latest = window.stream()
                    .sorted(Comparator.comparing(ArticleVO::getUpdatedDateTime, Comparator.reverseOrder())
                            .thenComparing(ArticleVO::getId, Comparator.reverseOrder()))
                    .limit(MAX_CACHE_SIZE)
                    .toList();
            addToRanking(temporaryRankingKeys.get(ArticleType.ALL.ordinal()), latest);

            articlesByType.forEach((type, articles) ->
                    addToRanking(temporaryRankingKeys.get(type), articles));

            temporaryKeys.forEach(key -> stringRedisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS));
            // Lua 会删除旧正式 key，并把临时 key 原子改名为正式 key
            swapCache(temporaryDetailsKey, temporaryRankingKeys);
        } finally {
            stringRedisTemplate.delete(temporaryKeys);
        }
    }


    private List<ArticleVO> queryPageFromDB(int start, Integer type, int size) {
        Integer databaseType = type == null || type == ArticleType.ALL.ordinal() ? null : type;
        return articleMapper.selectPage(start, databaseType, size);
    }

    private String rankingKey(Integer type) {
        int rankingType = type == null ? ArticleType.ALL.ordinal() : type;
        return RANKING_ARTICLES + ":" + rankingType;
    }

    private void addToRanking(String key, List<ArticleVO> articles) {
        if (articles.isEmpty()) {
            return;
        }

        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        articles.forEach(vo -> tuples.add(
                new DefaultTypedTuple<>(String.valueOf(vo.getId()), vo.getScore())));
        stringRedisTemplate.opsForZSet().add(key, tuples);
    }

    private void cacheArticle(ArticleVO vo, Integer oldType) {
        synchronized (cacheRebuildLock) {
            String articleId = String.valueOf(vo.getId());
            if (oldType != null && !Objects.equals(oldType, vo.getType())) {
                stringRedisTemplate.opsForZSet().remove(rankingKey(oldType), articleId);
            }

            stringRedisTemplate.opsForHash().put(CACHE_ARTICLES, articleId, JSONUtil.toJsonStr(vo));
            stringRedisTemplate.opsForZSet().add(rankingKey(vo.getType()), articleId, vo.getScore());
            stringRedisTemplate.opsForZSet().add(rankingKey(ArticleType.ALL.ordinal()), articleId, vo.getScore());
            // 新文章可能挤出榜单末尾文章，记录这些文章以便清理详情
            Set<String> evictedIds = new HashSet<>();
            evictedIds.addAll(trimRanking(rankingKey(vo.getType())));
            evictedIds.addAll(trimRanking(rankingKey(ArticleType.ALL.ordinal())));
            removeUnreferencedDetails(evictedIds);
        }
    }

    private Set<String> trimRanking(String key) {
        Long cacheSize = stringRedisTemplate.opsForZSet().size(key);
        if (cacheSize == null || cacheSize <= MAX_CACHE_SIZE) {
            return Set.of();
        }

        // ZSET 正序是从旧到新，因此从 0 开始删除最旧的文章
        long removeEnd = cacheSize - MAX_CACHE_SIZE - 1;
        Set<String> evictedIds = stringRedisTemplate.opsForZSet().range(key, 0, removeEnd);
        stringRedisTemplate.opsForZSet().removeRange(key, 0, removeEnd);
        return evictedIds == null ? Set.of() : evictedIds;
    }

    private void removeUnreferencedDetails(Set<String> articleIds) {
        if (articleIds.isEmpty()) {
            return;
        }

        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        Set<String> unreferencedIds = new HashSet<>();
        for (String articleId : articleIds) {
            boolean referenced = false;
            for (ArticleType articleType : ArticleType.values()) {
                if (ops.score(rankingKey(articleType.ordinal()), articleId) != null) {
                    referenced = true;
                    break;
                }
            }
            if (!referenced) {
                unreferencedIds.add(articleId);
            }
        }
        if (!unreferencedIds.isEmpty()) {
            stringRedisTemplate.opsForHash().delete(CACHE_ARTICLES,
                    unreferencedIds.toArray());
        }
    }

    private void swapCache(String temporaryDetailsKey, List<String> temporaryRankingKeys) {
        // Lua 脚本约定：1~8 为正式 key，9 为 ready key，10~17 为临时 key
        List<String> keys = new ArrayList<>();
        keys.add(CACHE_ARTICLES);
        for (ArticleType articleType : ArticleType.values()) {
            keys.add(rankingKey(articleType.ordinal()));
        }
        keys.add(CACHE_READY);
        keys.add(temporaryDetailsKey);
        keys.addAll(temporaryRankingKeys);
        stringRedisTemplate.execute(SWAP_CACHE_SCRIPT, keys, String.valueOf(CACHE_TTL_HOURS * 3600));
    }

    private static DefaultRedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
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

        // 先计算需要删除的文件，等数据库事务提交后再删除
        Set<String> oldObjectNames = extractObjectNames(oldArticle.getContent());
        Set<String> newObjectNames = extractObjectNames(articleDTO.getContent());
        List<String> toDelete = oldObjectNames.stream()
                .filter(name -> !newObjectNames.contains(name))
                .toList();

        // 更新文章
        Article article = BeanUtil.toBean(articleDTO, Article.class);
        article.setUpdatedDateTime(LocalDateTime.now());
        articleMapper.updateById(article);

        // 更新请求不直接写缓存，事务提交后删除整组缓存，由查询接口负责重建
        registerAfterCommit(() -> {
            clearArticleCache();
            stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
            if (!toDelete.isEmpty()) {
                commonService.delete(toDelete.toArray(String[]::new));
            }
        });
    }

    private void registerAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void clearArticleCache() {
        synchronized (cacheRebuildLock) {
            List<String> keys = new ArrayList<>();
            keys.add(CACHE_ARTICLES);
            for (ArticleType articleType : ArticleType.values()) {
                keys.add(rankingKey(articleType.ordinal()));
            }
            keys.add(CACHE_READY);
            stringRedisTemplate.delete(keys);
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
    public void delete(Long id) {
        Article article = this.getById(id);
        if (article == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        permissionService.checkOwnerOrAdminPermission(article.getStudentId());

        // 提取文章内容中的所有图片 objectName 并删除 OSS 文件
        deleteImages(article.getContent());

        articleMapper.deleteById(id);
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
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
        synchronized (cacheRebuildLock) {
            if (id != null && type != null && type >= 0) {
                String sId = String.valueOf(id);
                stringRedisTemplate.opsForZSet().remove(RANKING_ARTICLES + ":" + type, sId);
                stringRedisTemplate.opsForZSet().remove(RANKING_ARTICLES + ":" + ArticleType.ALL.ordinal(), sId);
                stringRedisTemplate.opsForHash().delete(CACHE_ARTICLES, sId);
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