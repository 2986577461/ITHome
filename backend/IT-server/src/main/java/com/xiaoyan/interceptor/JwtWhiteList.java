package com.xiaoyan.interceptor;


import com.xiaoyan.context.BaseContext;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


/**
 * @author yuchao
 */
@Component
@AllArgsConstructor
public class JwtWhiteList {

    private StringRedisTemplate stringRedisTemplate;

    private static final String HASH_KEY = "jwt:active_sessions";

    public boolean validation(Integer studentId, String token) {
        String storedtToken = (String) stringRedisTemplate.opsForHash().get(HASH_KEY, String.valueOf(studentId));
        return storedtToken != null && storedtToken.equals(token);
    }

    public void updateToken(String token) {
        Integer studentId = BaseContext.getCurrentStudentId();
        stringRedisTemplate.opsForHash().put(HASH_KEY, String.valueOf(studentId), token);
    }

    public void deleteToken(Object ... studentIds) {
        stringRedisTemplate.opsForHash().delete(HASH_KEY, studentIds);
    }
}