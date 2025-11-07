package com.community.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * 简单 JWT 工具类（HS256）。
 * ⚠️ 生产请把 SECRET 放配置中心/环境变量，过期时间按需调整。
 */
public class JwtUtil {

    private static final String SECRET = System.getenv().getOrDefault(
            "JWT_SECRET", "CommunityGroupSecretKey-ReplaceMe-AtLeast-32-Bytes"
    );

    private static final long DEFAULT_TTL_MS = 1000L * 60 * 60 * 24; // 1 天

    private static Key key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String role) {
        return generateToken(userId, role, DEFAULT_TTL_MS);
    }

    public static String generateToken(Long userId, String role, long ttlMillis) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expireAt = new Date(now + ttlMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of("role", role == null ? "" : role))
                .setIssuedAt(issuedAt)
                .setExpiration(expireAt)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Claims parseToken(String token) {
        String real = normalize(token);
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(real)
                .getBody();
    }

    private static String normalize(String token) {
        if (token == null) return "";
        token = token.trim();
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }

    public static Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public static String getRole(String token) {
        Object v = parseToken(token).get("role");
        return v == null ? "" : v.toString();
    }
}
