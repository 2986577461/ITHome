package com.xiaoyan.interceptor;


import com.xiaoyan.context.BaseContext;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


/**
 * @author yuchao
 */
@Component
@AllArgsConstructor
public class JwtWhiteList {

    private RedisTemplate<String, Object> redisTemplate;

    public boolean validation(Integer studentId, String token) {
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        String hashKey = "jwt:active_sessions";
        String storedtToken = (String) hash.get(hashKey, String.valueOf(studentId));
        return storedtToken != null && storedtToken.equals(token);
    }

    public void addOrUpdateTokenHash(String token) {
        Integer studentId = BaseContext.getCurrentStudentId();
        HashOperations<String, String, Object> opsForHash = redisTemplate.opsForHash();
        String hashKey = "jwt:active_sessions";
        opsForHash.put(hashKey, String.valueOf(studentId), token);
    }
}
