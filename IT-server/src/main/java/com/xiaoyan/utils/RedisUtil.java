package com.xiaoyan.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaoyan.baseinterface.HashCacheId;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    private static final long WAITING_MILL = 50;

    public static final long VOID_VALUE_TTL = 2L;

    public static final Long LOCK_TTL = 10L;
    public static Long DEFAULT_TTL = 120L;
    public static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;

    public static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    @Data
    public static class RedisData {
        private LocalDateTime expireTime;
        private Object data;
    }

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private final ExecutorService cacheRebuildExecutor = new ThreadPoolExecutor(
            10, // 核心线程数
            20, // 最大线程数
            60L, TimeUnit.SECONDS, // 线程空闲时间
            new LinkedBlockingQueue<>(100) // 任务队列
    );

    public void save(@NonNull String key, @NonNull Object value) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), DEFAULT_TTL, DEFAULT_TIME_UNIT);
    }

    public void putHash(@NonNull String key, @NonNull String hashKey, Object value) {
        stringRedisTemplate.opsForHash().put(key, hashKey, JSONUtil.toJsonStr(value));
    }

    public void saveWithLogicalExpire(@NonNull String key, @NonNull Object value) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(DEFAULT_TIME_UNIT.toSeconds(DEFAULT_TTL)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    private RedisData getCache(String key) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JSONUtil.toBean(json, RedisData.class);
    }

    private boolean veryfyExpire(RedisData data) {
        return data.getExpireTime().isAfter(LocalDateTime.now());
    }

    public <R, ID> R queryCountWithLogicalExpire(@NonNull String keyPerfix, @NonNull ID id,
                                                 @NonNull Class<R> rType, @NonNull Function<ID, R> dbFallback) {
        String key = keyPerfix + ":" + id;
        RedisData data = getCache(key);
        if (data == null) {
            return null;
        }
        R r1 = JSONUtil.toBean((String) data.getData(), rType);
        if (veryfyExpire(data)) {
            return r1;
        }
        String lockKey = "cache:" + keyPerfix;
        //如果没有获取到锁，直接返回旧数据
        if (!tryLock(lockKey)) {
            return r1;
        }
        //双重校验，当持有锁的线程更新数据到缓存后，将会是最新数据，逻辑过期时间不会超过现在，
        // 所以没必要接着查询数据库
        RedisData data1 = getCache(key);
        if (data1 == null) {
            return null;
        }
        R r2 = JSONUtil.toBean((String) data1.getData(), rType);
        if (veryfyExpire(data)) {
            return r2;
        }

        cacheRebuildExecutor.submit(() -> {
            try {
                // 执行重建缓存操作
                R r3 = dbFallback.apply(id);
                this.saveWithLogicalExpire(key, r3); // 假设过期时间为20秒
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // 释放锁
                unlock(lockKey);
            }
        });
        return r1;
    }

    public Long queryCountWithLogicalExpire(@NonNull String key, @NonNull Supplier<Long> dbFallback) {
        RedisData data = getCache(key);
        if (data == null) {
            return null;
        }
        Long count1 = Long.valueOf(String.valueOf(data.getData()));
        if (data.getExpireTime().isAfter(LocalDateTime.now())) {
            return count1;
        }

        String lockKey = "lock:" + key;
        //如果没有获取到锁，直接返回旧数据
        if (!tryLock(lockKey)) {
            return count1;
        }

        //双重校验，当持有锁的线程更新数据到缓存后，将会是最新数据，逻辑过期时间不会超过现在，
        // 所以没必要接着查询数据库
        RedisData data2 = getCache(key);
        if (data2 == null) {
            return null;
        }

        Long count2 = Long.valueOf(String.valueOf(data2.getData()));

        if (data2.getExpireTime().isAfter(LocalDateTime.now())) {
            return count2;
        }

        cacheRebuildExecutor.submit(() ->
        {
            try {
                // 执行重建缓存操作
                Long count3 = dbFallback.get();
                this.saveWithLogicalExpire(key, count3);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // 释放锁
                unlock(lockKey);
            }
        });
        return count1;
    }

    public <R, ID> R queryStringWithMutex(@NonNull String keyPerfix, @NonNull ID id,
                                          @NonNull Class<R> rType, @NonNull Function<ID, R> dbFallback) {
        String key = keyPerfix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //不为null、"" 、\n
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, rType);
        }
        //不为null
        if (json != null) {
            return null;
        }
        String lockKey = "lock:" + key;
        int i = 0;
        while (!tryLock(lockKey)) {
            try {
                Thread.sleep(WAITING_MILL);
                i++;
                if (i >= 20) {
                    throw new RuntimeException("服务繁忙，请稍后重试！");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            //双重校验，当持有锁的线程更新数据到缓存后，其他数据没必要接着查询数据库
            String json2 = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json2)) {
                return JSONUtil.toBean(json2, rType);
            }
            if (json2 != null) {
                return null;
            }
            R r = dbFallback.apply(id);
            if (r == null) {
                stringRedisTemplate.opsForValue().set(key, "", VOID_VALUE_TTL, TIME_UNIT);
                return null;
            }
            this.save(key, r);
            return r;
        } finally {
            this.unlock(lockKey);
        }
    }

    public <R> R queryHashWithMutex(@NonNull String key, @NonNull String hashKey,
                                    @NonNull Class<R> rType, @NonNull Function<String, R> dbFallback) {
        Object o = stringRedisTemplate.opsForHash().get(key, hashKey);
        if (StrUtil.isNotBlank((String) o)) {
            return JSONUtil.toBean((String) o, rType);
        }
        if (o != null) {
            return null;
        }
        String lockKey = "lock:" + key;
        int i = 0;
        while (!tryLock(lockKey)) {
            try {
                Thread.sleep(WAITING_MILL);
                i++;
                if (i >= 20) {
                    throw new RuntimeException("服务繁忙，请稍后重试！");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            //双重校验，当持有锁的线程更新数据到缓存后，其他数据没必要接着查询数据库
            Object o1 = stringRedisTemplate.opsForHash().get(key, hashKey);
            if (StrUtil.isNotBlank((String) o1)) {
                return JSONUtil.toBean((String) o1, rType);
            }
            if (o1 != null) {
                return null;
            }

            R r = dbFallback.apply(hashKey);
            if (r == null) {
                stringRedisTemplate.opsForValue().set(key, "", VOID_VALUE_TTL, TIME_UNIT);
                return null;
            }
            this.putHash(key, hashKey, r);
            return r;
        } finally {
            this.unlock(lockKey);
        }
    }

    private boolean tryLock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
        return b != null && b;
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    public <P extends HashCacheId, T> List<T> getAllWithHashCache(String cacheKey,
                                                                  Supplier<Long> countSupplier,
                                                                  Supplier<List<P>> dbFallback,
                                                                  Class<P> pojoType,
                                                                  Class<T> targetType) {
        // 1. 获取缓存总数
        long count = countSupplier.get();

        // 2. 查询缓存
        List<Object> caches = stringRedisTemplate.opsForHash().values(cacheKey);

        // 3. 校验缓存，如果数量匹配，则返回缓存数据
        if (caches.size() == count) {
            return caches.stream()
                    .map(o -> {
                        P p= JSONUtil.toBean((String) o, pojoType);
                        System.out.println(p);
                        return BeanUtil.toBean(p, targetType);
                    })
                    .toList();
        }

        // 4. 缓存不完整，从数据库获取数据
        stringRedisTemplate.delete(cacheKey);
        List<P> list = dbFallback.get();

        // 5. 存入缓存
        Map<String, String> map = new HashMap<>();
        list.forEach(p -> map.put(p.getCacheId(), JSONUtil.toJsonStr(p)));
        stringRedisTemplate.opsForHash().putAll(cacheKey, map);

        // 6. 返回数据
        return list.stream()
                .map(p -> BeanUtil.toBean(p, targetType))
                .toList();
    }
}