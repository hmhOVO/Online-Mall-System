package com.example.service;

import com.auth0.jwt.JWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Long> fallbackBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToBlacklist(String token) {
        Date expiresAt = JWT.decode(token).getExpiresAt();
        long ttl = expiresAt.getTime() - System.currentTimeMillis();
        if (ttl <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(PREFIX + token, "1", ttl, TimeUnit.MILLISECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存黑名单：{}", e.getMessage());
            fallbackBlacklist.put(token, expiresAt.getTime());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存黑名单：{}", e.getMessage());
            Long expireAt = fallbackBlacklist.get(token);
            if (expireAt == null) {
                return false;
            }
            if (expireAt < System.currentTimeMillis()) {
                fallbackBlacklist.remove(token);
                return false;
            }
            return true;
        }
    }
}
