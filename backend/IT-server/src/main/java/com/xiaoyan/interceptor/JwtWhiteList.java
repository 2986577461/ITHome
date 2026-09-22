package com.xiaoyan.interceptor;


import com.xiaoyan.context.BaseContext;
import com.xiaoyan.utils.RedisUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * @author yuchao
 */
@Component
@AllArgsConstructor
public class JwtWhiteList {

    private static final String HASH_KEY = "jwt:active_sessions";

    private RedisUtil redisUtil;

    /** Redis 不可用时放行，理由见 {@link RedisUtil#isTokenValid} */
    public boolean validation(String studentId, String token) {
        return redisUtil.isTokenValid(HASH_KEY, studentId, token);
    }

    public void updateToken(String token) {
        redisUtil.putHashField(HASH_KEY, BaseContext.getCurrentStudentId(), token);
    }

    public void deleteToken(Object... studentIds) {
        redisUtil.evictHashFields(HASH_KEY, studentIds);
    }
}
