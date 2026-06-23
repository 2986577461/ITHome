package com.xiaoyan.utils;

import cn.hutool.json.JSONUtil;
import com.xiaoyan.baseinterface.HashCacheId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisUtilTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @InjectMocks
    private RedisUtil redisUtil;

    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
    }

    /* ====================================================
     * save() — 基本写入
     * ==================================================== */
    @Nested
    class Save {
        @Test
        void should_set_value_with_default_ttl() {
            redisUtil.save("k", "hello");
            verify(valueOps).set(eq("k"), anyString(),
                    eq(RedisUtil.DEFAULT_TTL), eq(RedisUtil.DEFAULT_TIME_UNIT));
        }

        @Test
        void should_serialize_object_to_json() {
            redisUtil.save("k", new CacheValue(42));
            verify(valueOps).set("k", "{\"value\":42}",
                    RedisUtil.DEFAULT_TTL, RedisUtil.DEFAULT_TIME_UNIT);
        }
    }

    /* ====================================================
     * saveWithLogicalExpire() — 逻辑过期
     * ==================================================== */
    @Nested
    class SaveWithLogicalExpire {
        @Test
        void should_wrap_in_redis_data() {
            redisUtil.saveWithLogicalExpire("k", new CacheValue(1));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(valueOps).set(eq("k"), captor.capture());

            RedisUtil.RedisData saved = JSONUtil.toBean(captor.getValue(), RedisUtil.RedisData.class);
            assertNotNull(saved.getData());
            assertNotNull(saved.getExpireTime());
            assertTrue(saved.getExpireTime().isAfter(LocalDateTime.now()));
        }
    }

    /* ====================================================
     * queryStringWithMutex() — 互斥锁 + 双重校验
     * ==================================================== */
    @Nested
    class QueryStringWithMutex {
        @Test
        void should_return_cached_value_on_hit() {
            when(stringRedisTemplate.opsForValue().get("k1")).thenReturn("{\"value\":10}");

            CacheValue result = redisUtil.queryStringWithMutex("k", 1, CacheValue.class, id -> null);

            assertEquals(10, result.getValue());
        }

        @Test
        void should_return_null_when_cache_empty_string() {
            when(stringRedisTemplate.opsForValue().get("k1")).thenReturn("");

            CacheValue result = redisUtil.queryStringWithMutex("k", 1, CacheValue.class, id -> null);

            assertNull(result);
        }

        @Test
        void should_fallback_to_db_on_miss_and_cache_result() {
            when(stringRedisTemplate.opsForValue().get("k1")).thenReturn(null);
            when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

            CacheValue result = redisUtil.queryStringWithMutex("k", 1, CacheValue.class,
                    id -> new CacheValue(20));

            assertEquals(20, result.getValue());
            verify(valueOps).set("k1", "{\"value\":20}",
                    RedisUtil.DEFAULT_TTL, RedisUtil.DEFAULT_TIME_UNIT);
            verify(stringRedisTemplate).delete("lock:k1");
        }

        @Test
        void should_cache_void_value_when_db_returns_null() {
            when(stringRedisTemplate.opsForValue().get("k1")).thenReturn(null);
            when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

            CacheValue result = redisUtil.queryStringWithMutex("k", 1, CacheValue.class, id -> null);

            assertNull(result);
            verify(valueOps).set("k1", "",
                    RedisUtil.VOID_VALUE_TTL, RedisUtil.TIME_UNIT);
            verify(stringRedisTemplate).delete("lock:k1");
        }
    }

    /* ====================================================
     * queryHashWithMutex() — Hash 版互斥锁
     * ==================================================== */
    @Nested
    class QueryHashWithMutex {
        @Test
        void should_return_cached_value_on_hit() {
            when(hashOps.get("hash", "hk")).thenReturn("{\"value\":30}");

            CacheValue result = redisUtil.queryHashWithMutex("hash", "hk",
                    CacheValue.class, key -> null);

            assertEquals(30, result.getValue());
        }

        @Test
        void should_fallback_to_db_on_miss() {
            when(hashOps.get("hash", "hk")).thenReturn(null);
            when(valueOps.setIfAbsent("lock:hash", "1", 10L, TimeUnit.SECONDS))
                    .thenReturn(true);

            redisUtil.queryHashWithMutex("hash", "hk", CacheValue.class, key -> new CacheValue(40));

            verify(hashOps).put("hash", "hk", "{\"value\":40}");
        }

        @Test
        void should_double_check_and_skip_db_when_another_thread_just_updated_cache() {
            when(hashOps.get("hash", "hk"))
                    .thenReturn(null)
                    .thenReturn("{\"value\":50}");
            when(valueOps.setIfAbsent("lock:hash", "1", 10L, TimeUnit.SECONDS))
                    .thenReturn(true);

            CacheValue result = redisUtil.queryHashWithMutex("hash", "hk",
                    CacheValue.class, key -> {
                        throw new RuntimeException("不应该走到DB");
                    });

            assertEquals(50, result.getValue());
        }
    }

    /* ====================================================
     * getAllWithHashCache() — Hash 全量缓存
     * ==================================================== */
    @Nested
    class GetAllWithHashCache {
        @Test
        void should_return_cached_data_when_size_matches() {
            when(stringRedisTemplate.opsForHash().values("p"))
                    .thenReturn(List.of(
                            JSONUtil.toJsonStr(new Person("a", 1)),
                            JSONUtil.toJsonStr(new Person("b", 2))));

            List<Person> result = redisUtil.getAllWithHashCache("p",
                    () -> 2L, () -> { throw new RuntimeException("不应该走到DB"); }, Person.class);

            assertEquals(2, result.size());
        }

        @Test
        void should_load_from_db_when_cache_size_mismatch() {
            when(stringRedisTemplate.opsForHash().values("p")).thenReturn(List.of());

            List<Person> result = redisUtil.getAllWithHashCache("p",
                    () -> 1L, () -> List.of(new Person("c", 3)), Person.class);

            assertEquals(1, result.size());
            assertEquals("c", result.get(0).getName());
        }
    }

    /* ====================================================
     * 测试用 POJO
     * ==================================================== */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CacheValue {
        private int value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Person implements HashCacheId {
        private String name;
        private int age;
        @Override
        public String getCacheId() { return name; }
    }
}