package com.xiaoyan.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.UsersService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;

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
    private StringRedisTemplate stringRedisTemplate;
    private CommonService commonService;

    public static final Pattern IMAGE_PATTERN = Pattern.compile("https?://[^/]+\\.aliyuncs\\.com/([^\"'\\s]+)");

    @Override
    public Long getCount(Integer type) {
        LambdaQueryWrapper<Article> lqw = new LambdaQueryWrapper<>();
        if (type != null) {
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

        stringRedisTemplate.opsForZSet().add(CACHE_ARTICLES, JSONUtil.toJsonStr(vo), vo.getScore());
        stringRedisTemplate.opsForZSet().removeRange(CACHE_ARTICLES, 0, -(MAX_CACHE_SIZE + 1));
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
    public List<ArticleVO> getPage(@NonNull Integer page, Integer type, @NonNull Integer size, Integer studentId) {
        int start = (page - 1) * size;
        int end = page * size - 1;

        List<ArticleVO> result;

        if (end < MAX_CACHE_SIZE) {
            result = getPageFromCache(start, end, type, studentId, size);
            if (result != null && result.size() == size) {
                return result;
            }

            // 只有无过滤条件时重建缓存才有意义（重建的是全量50条）
            if (type == null && studentId == null) {
                buildLatestCache();
                result = getPageFromCache(start, end, null, null, size);
                if (result != null && result.size() == size) {
                    return result;
                }
            }
        }
        // 不在缓存范围内 / 缓存不够 → 数据库兜底
        result = queryPageFromDB(start, type, studentId, size);
        return result;
    }

    private List<ArticleVO> getPageFromCache(int start, int end, Integer type, Integer studentId, int size) {
        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        Long cacheSize = ops.size(CACHE_ARTICLES);

        if (cacheSize == null || cacheSize <= end) {
            return null;
        }

        if (type == null && studentId == null) {
            // 无过滤，直接取范围
            Set<String> set = ops.reverseRange(CACHE_ARTICLES, start, end);
            if (set != null && set.size() == size) {
                return set.stream().map(s -> JSONUtil.toBean(s, ArticleVO.class)).toList();
            }
        } else {
            // 有过滤：拉全量50条 → Java筛选 → 截取
            Set<String> set = ops.reverseRange(CACHE_ARTICLES, 0, -1);
            if (set != null) {
                List<ArticleVO> filtered = set.stream().map(s -> JSONUtil.toBean(s, ArticleVO.class))
                        .filter(vo -> (type == null || vo.getType().equals(type))
                                && (studentId == null || vo.getStudentId().equals(studentId))).toList();

                if (filtered.size() > start) {
                    int toIndex = Math.min(start + size, filtered.size());
                    List<ArticleVO> result = filtered.subList(start, toIndex);
                    if (result.size() == size) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    public void buildLatestCache() {
        // ① 查DB + 转换VO + 填姓名
        List<Article> list = articleMapper.selectPage(0, null, MAX_CACHE_SIZE);
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


    private List<ArticleVO> queryPageFromDB(int start, Integer type, Integer studentId, int size) {
        List<Article> list;
        if (studentId != null) {
            list = articleMapper.selectPageByStudentId(start, studentId, size);
        } else {
            list = articleMapper.selectPage(start, type, size);
        }
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
    @Transactional
    public void update(ArticleDTO articleDTO) {
        Article oldArticle = this.getById(articleDTO.getId());
        if (oldArticle == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        // 权限校验：仅作者或管理员可修改
        Integer studentId = BaseContext.getCurrentStudentId();
        StudentVO user = usersService.getUser(studentId);
        if (!JwtClaimsConstant.ADMIN_ID.equals(user.getPosition()) && !studentId.equals(oldArticle.getStudentId())) {
            throw new RuntimeException(MessageConstant.PERMISSION_DENIED);
        }

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

        // 重建缓存
        buildLatestCache();
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
        Integer studentId = BaseContext.getCurrentStudentId();
        if (article == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        StudentVO user = usersService.getUser(studentId);
        if (!JwtClaimsConstant.ADMIN_ID.equals(user.getPosition()) && !studentId.equals(article.getStudentId())) {
            throw new RuntimeException(MessageConstant.PERMISSION_DENIED);
        }
        // 提取文章内容中的所有图片 objectName 并删除 OSS 文件
        deleteImages(article.getContent());

        articleMapper.deleteById(id);
        // 从ZSET中精确移除该条
        removeFromCache(id);
    }


    private void deleteImages(String content) {
        Set<String> objectNames = extractObjectNames(content);
        if (!objectNames.isEmpty()) {
            commonService.delete(objectNames.toArray(String[]::new));
        }
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

    @Override
    public void deleteBatch(List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return;
        }

        // ② 批量删 OSS + 软删 student_file
        commonService.delete(objectNames.toArray(String[]::new));

    }
}