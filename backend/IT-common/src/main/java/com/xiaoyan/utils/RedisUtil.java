package com.xiaoyan.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.NonNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class RedisUtil implements DisposableBean {

    private static final long WAITING_MILL = 50;
    private static final int LOCK_RETRY_TIMES = 20;
    private static final String NEGATIVE_CACHE_PREFIX = "cache:null:";

    public static final long VOID_VALUE_TTL = 2L;
    public static final Long LOCK_TTL = 10L;
    public static Long DEFAULT_TTL = 120L;
    public static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;
    public static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = loadScript("lua/redis-unlock.lua");
    private static final DefaultRedisScript<Long> RENEW_LOCK_SCRIPT = loadScript("lua/redis-renew-lock.lua");

    private final StringRedisTemplate stringRedisTemplate;

    private final ExecutorService cacheRebuildExecutor = new ThreadPoolExecutor(
            10,
            20,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100));

    private final ScheduledExecutorService lockRenewalExecutor = new ScheduledThreadPoolExecutor(1);

    private record LockHandle(String key, String token, ScheduledFuture<?> renewalTask) {
    }

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void save(@NonNull String key, @NonNull Object value) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), DEFAULT_TTL, DEFAULT_TIME_UNIT);
    }
    @SuppressWarnings("unchecked")
    public <R> R queryStringWithMutex(@NonNull String key, @NonNull Class<?> type,
                                      @NonNull Supplier<R> dbFallback) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return (R) parse(json, type);
        }
        if (json != null) {
            return null;
        }

        LockHandle lock = acquireLockWithRetry("lock:string:" + key);
        try {
            String latest = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(latest)) {
                return (R) parse(latest, type);
            }
            if (latest != null) {
                return null;
            }

            R value = dbFallback.get();
            if (value == null) {
                cacheNull(key);
                return null;
            }
            save(key, value);
            return value;
        } finally {
            unlock(lock);
        }
    }

    private Object parse(String json, Class<?> type) {
        return json.trim().startsWith("[") ? JSONUtil.toList(json, type) : JSONUtil.toBean(json, type);
    }

    public <R> R queryHashWithMutex(@NonNull String key, @NonNull String hashKey,
                                    @NonNull Class<R> rType, @NonNull Function<String, R> dbFallback) {
        Object cached = stringRedisTemplate.opsForHash().get(key, hashKey);
        if (StrUtil.isNotBlank((String) cached)) {
            return JSONUtil.toBean((String) cached, rType);
        }
        if (cached != null || isNegativeCached(hashNullKey(key, hashKey))) {
            return null;
        }

        LockHandle lock = acquireLockWithRetry("lock:hash:" + key + ":" + hashKey);
        try {
            Object latest = stringRedisTemplate.opsForHash().get(key, hashKey);
            if (StrUtil.isNotBlank((String) latest)) {
                return JSONUtil.toBean((String) latest, rType);
            }
            if (latest != null || isNegativeCached(hashNullKey(key, hashKey))) {
                return null;
            }

            R value = dbFallback.apply(hashKey);
            if (value == null) {
                cacheNull(hashNullKey(key, hashKey));
                return null;
            }
            stringRedisTemplate.delete(hashNullKey(key, hashKey));
            stringRedisTemplate.opsForHash().put(key, hashKey, JSONUtil.toJsonStr(value));
            return value;
        } finally {
            unlock(lock);
        }
    }

    private String hashNullKey(String key, String hashKey) {
        return NEGATIVE_CACHE_PREFIX + "hash:" + key + ":" + hashKey;
    }


    private boolean isNegativeCached(String key) {
        return stringRedisTemplate.opsForValue().get(key) != null;
    }

    private void cacheNull(String key) {
        stringRedisTemplate.opsForValue().set(key, "", VOID_VALUE_TTL, TIME_UNIT);
    }


    private LockHandle acquireLockWithRetry(String key) {
        LockHandle lock;
        for (int attempt = 0; attempt < LOCK_RETRY_TIMES; attempt++) {
            lock = tryLock(key);
            if (lock != null) {
                return lock;
            }
            try {
                Thread.sleep(WAITING_MILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for cache lock", e);
            }
        }
        throw new IllegalStateException("服务繁忙，请稍后重试！");
    }

    private LockHandle tryLock(String key) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, LOCK_TTL, TimeUnit.SECONDS);
        if (acquired == null || !acquired) {
            return null;
        }

        try {
            long renewalInterval = Math.max(1L, TimeUnit.SECONDS.toMillis(LOCK_TTL) / 3);
            ScheduledFuture<?> renewalTask = lockRenewalExecutor.scheduleAtFixedRate(
                    () -> renewLock(key, token), renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
            return new LockHandle(key, token, renewalTask);
        } catch (RejectedExecutionException e) {
            releaseLock(key, token);
            return null;
        }
    }

    private void renewLock(String key, String token) {
        try {
            stringRedisTemplate.execute(RENEW_LOCK_SCRIPT, Collections.singletonList(key), token,
                    String.valueOf(TimeUnit.SECONDS.toMillis(LOCK_TTL)));
        } catch (RuntimeException ignored) {
            // The next renewal attempt may succeed while Redis is transiently unavailable.
        }
    }

    private void unlock(LockHandle lock) {
        if (lock == null) {
            return;
        }
        lock.renewalTask.cancel(false);
        releaseLock(lock.key, lock.token);
    }

    private void releaseLock(String key, String token) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
    }

    private static DefaultRedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public void destroy() {
        cacheRebuildExecutor.shutdown();
        lockRenewalExecutor.shutdown();
        try {
            if (!cacheRebuildExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cacheRebuildExecutor.shutdownNow();
            }
            if (!lockRenewalExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                lockRenewalExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cacheRebuildExecutor.shutdownNow();
            lockRenewalExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}