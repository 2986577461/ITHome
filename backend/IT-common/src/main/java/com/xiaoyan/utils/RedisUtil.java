package com.xiaoyan.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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

/**
 * 全项目唯一直接持有 StringRedisTemplate 的类。
 *
 * <p>每个方法自己处理 Redis 异常，调用方不用再写 try-catch。失败时的行为按用途分三类：</p>
 * <ul>
 *   <li><b>缓存读</b>（{@link #queryStringWithMutex} / {@link #queryHashWithMutex}）——
 *       降级查库。缓存只是「可选加速」，Redis 挂了系统应该只是变慢，不是不可用</li>
 *   <li><b>缓存写与失效</b>（{@link #putHashField} / {@link #evict} / {@link #evictHashFields}）——
 *       记日志后吞掉。最坏结果是脏数据多留到 TTL 过期</li>
 *   <li><b>登录态</b>（{@link #isTokenValid}）—— 放行。见该方法的注释</li>
 * </ul>
 *
 * <p>捕获的是 {@link DataAccessException}：Redis 连接失败、命令超时都是它的子类，
 * 而「抢锁重试耗尽」抛的 IllegalStateException 不属于它——那是主动的限流背压，应该继续往上抛。</p>
 *
 * @author yuchao
 */
@Component
@Slf4j
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

    /* ============================================================
     * 缓存读：Redis 不可用时降级查库
     * ============================================================ */

    /** 写缓存。失败只记日志：没写进去最坏是下次再查一次库，不该让业务失败 */
    public void save(@NonNull String key, @NonNull Object value) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, JSONUtil.toJsonStr(value), DEFAULT_TTL, DEFAULT_TIME_UNIT);
        } catch (DataAccessException e) {
            logDegraded("跳过写缓存", "key=" + key, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R queryStringWithMutex(@NonNull String key, @NonNull Class<?> type,
                                      @NonNull Supplier<R> dbFallback) {
        try {
            return doQueryStringWithMutex(key, type, dbFallback);
        } catch (DataAccessException e) {
            logDegraded("降级查库", "key=" + key, e);
            return dbFallback.get();
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R doQueryStringWithMutex(String key, Class<?> type, Supplier<R> dbFallback) {
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
        try {
            return doQueryHashWithMutex(key, hashKey, rType, dbFallback);
        } catch (DataAccessException e) {
            logDegraded("降级查库", "key=" + key + " field=" + hashKey, e);
            return dbFallback.apply(hashKey);
        }
    }

    private <R> R doQueryHashWithMutex(String key, String hashKey,
                                       Class<R> rType, Function<String, R> dbFallback) {
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
            evict(hashNullKey(key, hashKey));
            putHashField(key, hashKey, JSONUtil.toJsonStr(value));
            return value;
        } finally {
            unlock(lock);
        }
    }

    /* ============================================================
     * 缓存读写与失效：失败只记日志，等 TTL 自然过期
     * ============================================================ */

    /** 读 Hash field。Redis 不可用和字段不存在都返回 null，调用方一律按「未命中」处理 */
    public String getHashField(@NonNull String key, @NonNull String field) {
        try {
            return (String) stringRedisTemplate.opsForHash().get(key, field);
        } catch (DataAccessException e) {
            logDegraded("按未命中处理", "key=" + key + " field=" + field, e);
            return null;
        }
    }

    /** 写 Hash field，不设过期。用于整组失效的缓存（如按 id 存的学员信息） */
    public void putHashField(@NonNull String key, @NonNull String field, @NonNull String value) {
        try {
            stringRedisTemplate.opsForHash().put(key, field, value);
        } catch (DataAccessException e) {
            logDegraded("跳过写缓存", "key=" + key + " field=" + field, e);
        }
    }

    /** 写 Hash field 并刷新整个 key 的 TTL。用于分页缓存这类整组共享一个过期时间的场景 */
    public void putHashField(@NonNull String key, @NonNull String field, @NonNull String value,
                             long ttl, @NonNull TimeUnit unit) {
        try {
            stringRedisTemplate.opsForHash().put(key, field, value);
            stringRedisTemplate.expire(key, ttl, unit);
        } catch (DataAccessException e) {
            logDegraded("跳过写缓存", "key=" + key + " field=" + field, e);
        }
    }

    /** 删除缓存 key。失败只记日志：最坏是脏数据多留到 TTL 过期，远好于让接口报错 */
    public void evict(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            stringRedisTemplate.delete(Arrays.asList(keys));
        } catch (DataAccessException e) {
            logDegraded("缓存失效失败，等 TTL 过期", "keys=" + Arrays.toString(keys), e);
        }
    }

    /** 删除 Hash 里的若干 field */
    public void evictHashFields(@NonNull String key, Object... fields) {
        if (fields == null || fields.length == 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForHash().delete(key, fields);
        } catch (DataAccessException e) {
            logDegraded("缓存字段失效失败", "key=" + key + " fields=" + Arrays.toString(fields), e);
        }
    }

    /* ============================================================
     * 登录态白名单：不可降级
     *
     * 这里没有「降级查库」这个选项——白名单的意义就是「注销即失效」，
     * 数据库里根本没有这个状态。只能二选一：
     *   fail-open  —— 放行。服务可用，代价是已注销的 token 在 Redis 恢复前仍然有效
     *   fail-close —— 拒绝。安全，代价是所有人被判为未登录，等于全站不可用
     *
     * 取 fail-open：JWT 签名本身已能证明身份，白名单只是「让旧 token 提前失效」的增强，
     * 拿「注销延迟生效」换「全站可用」是划算的。
     * ============================================================ */

    /** 校验 token 是否为该学员当前有效的登录态。Redis 不可用时返回 true（放行） */
    public boolean isTokenValid(@NonNull String key, @NonNull String studentId, @NonNull String token) {
        String stored;
        try {
            stored = (String) stringRedisTemplate.opsForHash().get(key, studentId);
        } catch (DataAccessException e) {
            logDegraded("登录态校验失败，放行", "key=" + key + " studentId=" + studentId, e);
            return true;
        }
        return stored != null && stored.equals(token);
    }

    /* ============================================================
     * 分布式锁：内部使用
     * ============================================================ */

    private String hashNullKey(String key, String hashKey) {
        return NEGATIVE_CACHE_PREFIX + "hash:" + key + ":" + hashKey;
    }

    private boolean isNegativeCached(String key) {
        return stringRedisTemplate.opsForValue().get(key) != null;
    }

    private void cacheNull(String key) {
        try {
            stringRedisTemplate.opsForValue().set(key, "", VOID_VALUE_TTL, TIME_UNIT);
        } catch (DataAccessException e) {
            logDegraded("跳过写空值缓存", "key=" + key, e);
        }
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
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        } catch (DataAccessException e) {
            // 必须吞掉：这行在 finally 里执行，抛出去会顶掉降级路径的返回值，让整个降级失效。
            // 锁本身带 TTL，没人删也会自己过期。
            logDegraded("释放锁失败，等 TTL 过期", "key=" + key, e);
        }
    }

    /**
     * 降级日志。
     *
     * <p>故意不打堆栈。Redis 不可用是「已经处理掉的预期内故障」，不是意外：
     * 堆栈从 {@code RedisUtil} 往下全是 Lettuce / Spring 的库内调用，对定位没有帮助，
     * 而每个请求都会走到这里，打堆栈等于按请求刷屏。
     * 取根因的类名加 message 就够定位了——连接被拒、超时、还是命令报错，一眼能分。</p>
     */
    private void logDegraded(String action, String detail, DataAccessException e) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        log.warn("Redis 不可用，{} {} 根因={}: {}", action, detail,
                root.getClass().getSimpleName(), root.getMessage());
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
