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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

        stringRedisTemplate.opsForZSet().add(RANKING_ARTICLES + ":" + article.getType(),
                JSONUtil.toJsonStr(article.getId()), vo.getScore());
        stringRedisTemplate.opsForZSet().add(RANKING_ARTICLES + ":" + ArticleType.ALL.ordinal(),
                JSONUtil.toJsonStr(article.getId()), vo.getScore());

        stringRedisTemplate.opsForHash().put(CACHE_ARTICLES, String.valueOf(article.getId()), JSONUtil.toJsonStr(vo));
        stringRedisTemplate.opsForZSet().removeRange(CACHE_ARTICLES, 0, -(MAX_CACHE_SIZE + 1));
    }

    @Override
    public List<ArticleVO> getMyPage(@NonNull Integer page, @NonNull Integer size) {
        return List.of();
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

        int start = (page - 1) * size;
        int end = page * size - 1;

        List<ArticleVO> result;

        if (end < MAX_CACHE_SIZE) {
            result = getPageFromCache(start, end, type);
            if (result != null && result.size() == size) {
                return result;
            }
            buildLatestCache();

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
        Long cacheSize = ops.size(RANKING_ARTICLES);

        if (cacheSize == null || cacheSize <= end) {
            return null;
        }
//        if (studentId == null) {
        // 无过滤，直接取范围
        Set<String> set = ops.reverseRange(RANKING_ARTICLES + ":" + type, start, end);
        if (set == null) {
            return new ArrayList<>();
        }
        List<Object> list = stringRedisTemplate.opsForHash().multiGet(CACHE_ARTICLES, new ArrayList<>(set));
        return list.stream().map(s -> BeanUtil.toBean(s, ArticleVO.class)).toList();

//        } else {
//            // 有过滤：拉全量50条 → Java筛选 → 截取
//            Set<String> set = ops.reverseRange(CACHE_ARTICLES, 0, -1);
//            if (set != null) {
//                List<ArticleVO> filtered = set.stream().map(s -> JSONUtil.toBean(s, ArticleVO.class))
//                        .filter(vo -> (type == null || vo.getType().equals(type))
//                                && (studentId == null || vo.getStudentId().equals(studentId))).toList();
//
//                if (filtered.size() > start) {
//                    int toIndex = Math.min(start + size, filtered.size());
//                    List<ArticleVO> result = filtered.subList(start, toIndex);
//                    if (result.size() == size) {
//                        return result;
//                    }
//                }
//            }
//        }
//        return null;
    }

    public void buildLatestCache() {
        // 先清空再写入（保证是干净快照），设TTL防止僵尸缓存
        stringRedisTemplate.delete(CACHE_ARTICLES);
        stringRedisTemplate.delete(RANKING_ARTICLES);

        // 查DB + 转换VO + 填姓名
        List<Article> window = articleMapper.selectWindow(MAX_CACHE_SIZE);

        List<ArticleVO> vos = toArticleVOList(window);

        Map<Integer, List<ArticleVO>> map = new HashMap<>();
        for (ArticleVO vo : vos) {
            map.computeIfAbsent(vo.getType(), key -> new ArrayList<>())
                    .add(vo);
        }

        map.forEach((type, list) -> {
            Set<ZSetOperations.TypedTuple<String>> set = new HashSet<>();
            list.forEach(vo -> {
                        set.add(new DefaultTypedTuple<>(String.valueOf(vo.getId()), vo.getScore()));
                        stringRedisTemplate.opsForHash().put(CACHE_ARTICLES, String.valueOf(vo.getId()),
                                JSONUtil.toJsonStr(vo));
                    }
            );
            stringRedisTemplate.opsForZSet().add(RANKING_ARTICLES + ":" + type, set);

        });

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

        removeFromCache(oldArticle.getId(), oldArticle.getType());
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