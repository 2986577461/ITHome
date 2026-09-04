package com.xiaoyan.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static SecretKey toKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成jwt
     *
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims    设置的信息
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        Date exp = new Date(System.currentTimeMillis() + ttlMillis);

        return Jwts.builder()
                .claims(claims)
                .expiration(exp)
                .signWith(toKey(secretKey))
                .compact();
    }

    /**
     * 解析token
     *
     * @param secretKey jwt秘钥
     * @param token     加密后的token
     */
    public static Claims parseJWT(String secretKey, String token) {
        return Jwts.parser()
                .verifyWith(toKey(secretKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}