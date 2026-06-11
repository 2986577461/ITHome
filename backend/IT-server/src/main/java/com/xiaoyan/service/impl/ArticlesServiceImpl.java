package com.xiaoyan.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.ArticleImageVO;
import com.xiaoyan.vo.ArticleVO;
import com.xiaoyan.vo.StudentVO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_COUNT_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;

@Service
@AllArgsConstructor
public class ArticlesServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticlesService {

    /**
     * ZSET缓存最多保留的文章数
     */
    public static final int MAX_CACHE_SIZE = 50;
    private final UsersService usersService;
    private ArticleMapper articleMapper;
    private UserMapper userMapper;
    private StringRedisTemplate stringRedisTemplate;
    private CommonService commonService;
    private StudentFileMapper studentFileMapper;

    @Override
    public Long getCount(Integer type) {
        LambdaQueryWrapper<Article> lqw = new LambdaQueryWrapper<>();
        if (type != null) {
            lqw.eq(Article::getType, type);
        }
        return this.count(lqw);
    }

    @Override
    @Transactional
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

        stringRedisTemplate.opsForZSet().add(CACHE_ARTICLES,
                JSONUtil.toJsonStr(vo), vo.getScore());
        stringRedisTemplate.opsForZSet().removeRange(CACHE_ARTICLES, 0, -(MAX_CACHE_SIZE + 1));

        userMapper.addArticleCountById(studentId);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
        stringRedisTemplate.delete(CACHE_COUNT_ARTICLES);
    }

    /**
     * 分页查询文章。
     *
     * <h3>两条路径</h3>
     * <pre>
     * 请求 page=3, type=1, size=5  →  start=10, end=14
     *
     * end < 50（该页在最新50条范围内）？
     *   ├─ 是 → 尝试从Redis ZSET取
     *   │       ├─ 取到满一页 → 直接返回
     *   │       └─ 没取到 →
     *   │            ├─ type=null → 重建缓存（查最新50条写回Redis），再试一次
     *   │            └─ type!=null → 跳过重建（ZSET里该type就这么多，重建也白建），走数据库
     *   │
     *   └─ 否 → 不在缓存范围内，直接查数据库
     * </pre>
     */
    @Override
    public List<ArticleVO> getPage(@NonNull Integer page, Integer type, @NonNull Integer size) {
        int start = (page - 1) * size;
        int end = page * size - 1;

        List<ArticleVO> result;

        if (end < MAX_CACHE_SIZE) {
            result = getPageFromCache(start, end, type, size);
            if (result != null && result.size() == size) {
                return result;
            }

            if (type == null) {
                buildLatestCache(null);
                result = getPageFromCache(start, end, null, size);
                if (result != null && result.size() == size) {
                    return result;
                }
            }
        }
        // 不在缓存范围内 / 缓存不够 → 数据库兜底
        result = queryPageFromDB(start, type, size);
        return result;
    }

    private List<ArticleVO> getPageFromCache(int start, int end, Integer type, int size) {
        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        Long cacheSize = ops.size(CACHE_ARTICLES);

        if (cacheSize == null || cacheSize <= end) {
            return null;
        }

        if (type == null) {
            Set<String> set = ops.reverseRange(CACHE_ARTICLES, start, end);
            if (set != null) {
                List<ArticleVO> result = set.stream()
                        .map(s -> JSONUtil.toBean(s, ArticleVO.class))
                        .toList();
                // 条数必须刚好等于size才返回。不够说明缓存数据有缺口，返回null走DB
                if (result.size() == size) {
                    return result;
                }
            }
        } else {
            // ===== 按type筛选：全量拉出来，Java过滤，再截取 =====
            // ZREVRANGE 0 -1：把ZSET里全部50条取出来
            Set<String> set = ops.reverseRange(CACHE_ARTICLES, 0, -1);
            if (set != null) {
                // 第1步：反序列化 + 按type过滤
                List<ArticleVO> filtered = set.stream()
                        .map(s -> JSONUtil.toBean(s, ArticleVO.class))
                        .filter(vo -> vo.getType().equals(type))
                        .toList();

                /*
                 * 第2步：在过滤后的结果里，截取该页。
                 *
                 * 注意：start 是"全局最新50条"中的偏移，但过滤后列表（filtered）短了很多。
                 * 所以不能直接把 (start, size) 当 filtered 的索引。
                 *
                 * 例：ZSET有50条，其中type=1有8条
                 *   filtered = [第3条, 第7条, 第12条, ...]（共8条，按score降序）
                 *
                 *   - 请求 page=1, size=5 → start=0：filtered.size()=8 > 0 ✓
                 *       subList(0, min(0+5,8)) = subList(0,5) → 5条刚好 → 返回
                 *   - 请求 page=2, size=5 → start=5：filtered.size()=8 > 5 ✓
                 *       subList(5, min(5+5,8)) = subList(5,8) → 3条 ≠ 5 → 返回null
                 *      （该type总共才8条，第2页只有3条是正常的，走DB也能得到同样结果）
                 */
                if (filtered.size() > start) {
                    int toIndex = Math.min(start + size, filtered.size());
                    List<ArticleVO> result = filtered.subList(start, toIndex);
                    if (result.size() == size) {
                        return result;
                    }
                }
                // filtered.size() <= start：该type的数量还没有start位置多，
                // 说明这一页压根不存在（例如type=1只有3条，用户翻到了第3页start=10）
            }
        }
        return null;
    }

    public void buildLatestCache(Integer type) {
        // ① 查DB + 转换VO + 填姓名
        List<Article> list = articleMapper.selectPage(0, type, MAX_CACHE_SIZE);
        List<ArticleVO> vos = toArticleVOList(list);

        // ② 构建 TypedTuple 集合（每个Tuple = value + score）
        //    score = updatedDateTime 的时间戳，score越大排名越靠前（ZREVRANGE时先返回）
        Set<ZSetOperations.TypedTuple<String>> set = new HashSet<>();
        vos.forEach(vo ->
                set.add(new DefaultTypedTuple<>(JSONUtil.toJsonStr(vo), vo.getScore()))
        );

        // ③ 先清空再写入（保证是干净快照），设TTL防止僵尸缓存
        stringRedisTemplate.delete(CACHE_ARTICLES);
        if (!set.isEmpty()) {
            stringRedisTemplate.opsForZSet().add(CACHE_ARTICLES, set);
        }
        stringRedisTemplate.expire(CACHE_ARTICLES, 2, TimeUnit.HOURS);
    }


    private List<ArticleVO> queryPageFromDB(int start, Integer type, int size) {
        List<Article> list = articleMapper.selectPage(start, type, size);
        return toArticleVOList(list);
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
    public void update(ArticleDTO articleDTO) {
        Article article = BeanUtil.toBean(articleDTO, Article.class);
        article.setUpdatedDateTime(LocalDateTime.now());
        articleMapper.updateById(article);
        // 更新可能导致score变化，直接重建最新50条缓存
        buildLatestCache(null);
    }

    /**
     * 分页查询自己的文章。
     *
     * <p>逻辑跟 {@link #getPage} 一样（缓存最新50条 + 超出走DB），
     * 区别是筛选条件从 type 换成 studentId</p>。
     *
     * <pre>
     * end < 50（该页在最新50条范围内）？
     *   ├─ 是 → 从Redis ZSET全量拉出 → Java按studentId过滤 → 截取分页
     *   │       ├─ 取满一页 → 直接返回
     *   │       └─ 没取满 → 重建缓存再试 → 还不够 → 走DB
     *   └─ 否 → 不在缓存范围内，直接查DB
     * </pre>
     */
    @Override
    public List<ArticleVO> getMyArticles(@NonNull Integer page, @NonNull Integer size, Integer studentId) {
        int start = (page - 1) * size;
        int end = page * size - 1;

        List<ArticleVO> result;

        if (end < MAX_CACHE_SIZE) {
            result = getMyArticlesFromCache(start, end, studentId, size);
            if (result != null && result.size() == size) {
//                fillImageUrls(result);
                return result;
            }
            buildLatestCache(null);
            result = getMyArticlesFromCache(start, end, studentId, size);
            if (result != null && result.size() == size) {
//                fillImageUrls(result);
                return result;
            }
        }

        result = queryMyArticlesFromDB(start, studentId, size);
//        fillImageUrls(result);
        return result;
    }

    /**
     * 从 ZSET 中取该用户的一页文章。
     * ZREVRANGE 0 -1 拉全量 → Java 按 studentId 过滤 → 截取分页。
     *
     * <p>逻辑跟 {@link #getPageFromCache} 的 type!=null 分支完全一样，
     * 只是 filter 条件从 type 换成 studentId。</p>
     */
    private List<ArticleVO> getMyArticlesFromCache(int start, int end, Integer studentId, int size) {
        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        Long cacheSize = ops.size(CACHE_ARTICLES);

        if (cacheSize == null || cacheSize <= end) {
            return null;
        }

        // ZREVRANGE 0 -1：拉全量50条
        Set<String> set = ops.reverseRange(CACHE_ARTICLES, 0, -1);
        if (set != null) {
            // 反序列化 + 按studentId过滤
            List<ArticleVO> filtered = set.stream()
                    .map(s -> JSONUtil.toBean(s, ArticleVO.class))
                    .filter(vo -> vo.getStudentId() != null && vo.getStudentId().equals(studentId))
                    .toList();

            // 在过滤结果中截取该页
            if (filtered.size() > start) {
                int toIndex = Math.min(start + size, filtered.size());
                List<ArticleVO> result = filtered.subList(start, toIndex);
                if (result.size() == size) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 直接查数据库，按 studentId 分页，不写 Redis。
     */
    private List<ArticleVO> queryMyArticlesFromDB(int start, Integer studentId, int size) {
        List<Article> list = articleMapper.selectPageByStudentId(start, studentId, size);
        return toArticleVOList(list);
    }

    @Override
    public Integer getArticlePage(Long articleId, @NonNull Integer size) {
        /*
         * 思路：文章列表按 updated_date_time DESC 排序。
         * 统计有多少篇文章排在该文章前面（rank），
         * 则 page = rank / size + 1。
         *
         * 例：size=5
         *   rank=0（最新）  → 0/5 + 1 = 1  第1页
         *   rank=4（第5篇） → 4/5 + 1 = 1  第1页
         *   rank=5（第6篇） → 5/5 + 1 = 2  第2页
         *   rank=9（第10篇）→ 9/5 + 1 = 2  第2页
         */
        Article article = this.getById(articleId);
        if (article == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        // 统计排在它前面的文章数（updated_date_time 更大，或时间相同时 id 更大）
        long rank = articleMapper.countBefore(article.getUpdatedDateTime(), article.getId());
        return (int) (rank / size) + 1;
    }

    @Override
    public void delete(Long id) {
        Article article = this.getById(id);
        Integer studentId = BaseContext.getCurrentStudentId();
        if (article == null || !studentId.equals(article.getStudentId())) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        articleMapper.deleteById(id);
        userMapper.decreaceArticleCount(studentId);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
        stringRedisTemplate.delete(CACHE_COUNT_ARTICLES);

        // 从ZSET中精确移除该条
        removeFromCache(id);
    }

    /**
     * 从ZSET中移除指定文章（遍历最多50条，根据id匹配）。
     */
    private void removeFromCache(Long articleId) {
        Set<String> set = stringRedisTemplate.opsForZSet().range(CACHE_ARTICLES, 0, -1);
        if (set != null) {
            for (String json : set) {
                ArticleVO vo = JSONUtil.toBean(json, ArticleVO.class);
                if (vo.getId().equals(articleId)) {
                    stringRedisTemplate.opsForZSet().remove(CACHE_ARTICLES, json);
                    break;
                }
            }
        }
    }

    @Override
    public List<ArticleImageVO> batchUploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<ArticleImageVO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Long studentFileId = commonService.upload(file);
                // upload 只返回了 id，需要通过 mapper 查一下拿 url
                StudentFile sf = studentFileMapper.selectById(studentFileId);
                results.add(new ArticleImageVO(studentFileId, sf != null ? sf.getFileUrl() : null));
            } catch (IOException e) {
                throw new RuntimeException("上传文件失败: " + file.getOriginalFilename(), e);
            }
        }
        return results;
    }

    @Override
    public void deleteBatch(List<Long> studentFileIds) {
        if (studentFileIds == null || studentFileIds.isEmpty()) {
            return;
        }

        // ① 查所有要删的 StudentFile，收集 objectName
        List<StudentFile> files = studentFileMapper.selectBatchIds(studentFileIds);
        if (files.isEmpty()) {
            return;
        }
        List<String> objectNames = files.stream()
                .map(StudentFile::getObjectName)
                .toList();

        // ② 批量删 OSS + 软删 student_file
        commonService.delete(objectNames.toArray(new String[0]));

    }
}