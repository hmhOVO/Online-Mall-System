package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;
    private static final long ATTEMPT_EXPIRE_MINUTES = 30;

    private static final String FAIL_COUNT_PREFIX = "login:fail:count:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate redisTemplate;

    private final Map<String, AtomicInteger> fallbackFailCounter = new ConcurrentHashMap<>();
    private final Map<String, Long> fallbackLockMap = new ConcurrentHashMap<>();

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void loginFailed(String username) {
        String countKey = FAIL_COUNT_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(countKey);
            if (count != null && count == 1) {
                redisTemplate.expire(countKey, ATTEMPT_EXPIRE_MINUTES, TimeUnit.MINUTES);
            }
            if (count != null && count >= MAX_ATTEMPTS) {
                String lockKey = LOCK_PREFIX + username;
                redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
                log.warn("用户 {} 连续登录失败 {} 次，账号已锁定 {} 分钟", username, MAX_ATTEMPTS, LOCK_DURATION_MINUTES);
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存计数：{}", e.getMessage());
            AtomicInteger counter = fallbackFailCounter.computeIfAbsent(username, k -> new AtomicInteger(0));
            int count = counter.incrementAndGet();
            if (count >= MAX_ATTEMPTS) {
                fallbackLockMap.put(username, System.currentTimeMillis() + LOCK_DURATION_MINUTES * 60 * 1000);
                log.warn("用户 {} 连续登录失败 {} 次，账号已锁定 {} 分钟（内存模式）", username, MAX_ATTEMPTS, LOCK_DURATION_MINUTES);
            }
        }
    }

    public void loginSucceeded(String username) {
        String countKey = FAIL_COUNT_PREFIX + username;
        String lockKey = LOCK_PREFIX + username;
        try {
            redisTemplate.delete(countKey);
            redisTemplate.delete(lockKey);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存计数：{}", e.getMessage());
            fallbackFailCounter.remove(username);
            fallbackLockMap.remove(username);
        }
    }

    public boolean isLocked(String username) {
        String lockKey = LOCK_PREFIX + username;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存计数：{}", e.getMessage());
            Long lockExpireTime = fallbackLockMap.get(username);
            if (lockExpireTime == null) {
                return false;
            }
            if (lockExpireTime < System.currentTimeMillis()) {
                fallbackLockMap.remove(username);
                fallbackFailCounter.remove(username);
                return false;
            }
            return true;
        }
    }

    public int getFailCount(String username) {
        String countKey = FAIL_COUNT_PREFIX + username;
        try {
            String count = redisTemplate.opsForValue().get(countKey);
            return count == null ? 0 : Integer.parseInt(count);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存计数：{}", e.getMessage());
            AtomicInteger counter = fallbackFailCounter.get(username);
            return counter == null ? 0 : counter.get();
        }
    }

    public long getLockRemainingMinutes(String username) {
        String lockKey = LOCK_PREFIX + username;
        try {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            if (ttl == null || ttl <= 0) return 0;
            return ttl / 60 + (ttl % 60 > 0 ? 1 : 0);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存计数：{}", e.getMessage());
            Long lockExpireTime = fallbackLockMap.get(username);
            if (lockExpireTime == null) return 0;
            long remaining = lockExpireTime - System.currentTimeMillis();
            if (remaining <= 0) return 0;
            return remaining / 60000 + (remaining % 60000 > 0 ? 1 : 0);
        }
    }

    public void unlockUser(String username) {
        String countKey = FAIL_COUNT_PREFIX + username;
        String lockKey = LOCK_PREFIX + username;
        try {
            redisTemplate.delete(countKey);
            redisTemplate.delete(lockKey);
            log.info("管理员解锁用户：{}", username);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级使用内存计数：{}", e.getMessage());
            fallbackFailCounter.remove(username);
            fallbackLockMap.remove(username);
        }
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    public long getLockDurationMinutes() {
        return LOCK_DURATION_MINUTES;
    }
}
