package com.xiaoyan.cache;

import com.xiaoyan.enumeration.ArticleType;
import com.xiaoyan.utils.RedisUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES_READY;
import static com.xiaoyan.constant.RedisConstant.LOCK_ARTICLE_CACHE;
import static com.xiaoyan.constant.RedisConstant.RANKING_ARTICLES;

/**
 * 文章缓存的失效与加锁入口。
 *
 * <p>缓存 key 的拼装和分布式锁原先散落在 ArticlesService 和 UsersService 里各写一份，
 * 两边一旦不一致就会出现「清了 A 没清 B」的脏缓存，统一收敛到这里。</p>
 */
@Component
@AllArgsConstructor
@Slf4j
public class ArticleCacheManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisUtil redisUtil;

    /** 失效整组文章缓存，由查询接口负责重建。 */
    public void clear() {
        runWithLock(() -> stringRedisTemplate.delete(allCacheKeys()));
    }

    /**
     * 在文章缓存的分布式锁内执行写操作。
     *
     * <p>锁不可重入：action 内部不要再调用本方法或 {@link RedisUtil#executeWithLock}。</p>
     */
    public void runWithLock(Runnable action) {
        try {
            redisUtil.executeWithLock(LOCK_ARTICLE_CACHE, action);
        } catch (RuntimeException e) {
            // 缓存写失败不能影响主流程：数据已经在库里，缓存等过期后自然重建
            log.error("文章缓存写操作失败，等待缓存过期后自动重建", e);
        }
    }

    /** 文章缓存用到的全部 key：详情 Hash、各分类榜单 ZSET、构建完成标记。 */
    public static List<String> allCacheKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(CACHE_ARTICLES);
        for (ArticleType articleType : ArticleType.values()) {
            keys.add(RANKING_ARTICLES + ":" + articleType.getCode());
        }
        keys.add(CACHE_ARTICLES_READY);
        return keys;
    }
}
