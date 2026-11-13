package com.lxw.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

/**
 * JWT 生成与解析工具
 */
public class JwtUtil {

    /**
     * 生成 jwt
     *
     * @param secretKey 密钥
     * @param ttlMillis 过期时间（毫秒）
     * @param claims    自定义声明
     * @return token
     */
    public static String createJWT(String secretKey, Long ttlMillis, Map<String, Object> claims) {
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .setExpiration(exp)
                .compact();
    }

    /**
     * 解析 jwt
     *
     * @param secretKey 密钥
     * @param token     token
     * @return Claims
     */
    public static Claims parseJWT(String secretKey, String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }
}
