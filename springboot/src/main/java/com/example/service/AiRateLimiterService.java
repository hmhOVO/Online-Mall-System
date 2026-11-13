package com.example.service;

import com.example.entity.SysConfig;
import com.example.exception.CustomerException;
import com.example.mapper.SysConfigMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AiRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimiterService.class);
    private static final String PREFIX = "ai.rate.";

    private final SysConfigMapper sysConfigMapper;

    @Value("${ai.rate.task_split:10}")
    private int defaultTaskSplit;
    @Value("${ai.rate.risk_analyze:10}")
    private int defaultRiskAnalyze;

    private final Cache<String, TokenBucket> bucketCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public AiRateLimiterService(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    public void checkLimit(Integer userId, String scene) {
        String key = PREFIX + scene.toLowerCase();
        double limitPerMinute = getLimit(key);
        String cacheKey = userId + ":" + scene;
        TokenBucket bucket = getBucket(cacheKey, limitPerMinute);
        if (!bucket.tryAcquire()) {
            log.warn("用户 {} 在场景 {} 下触发 AI 限流（限制 {} 次/分钟）", userId, scene, limitPerMinute);
            throw new CustomerException("AI 调用过于频繁，请稍后再试");
        }
    }

    private TokenBucket getBucket(String cacheKey, double capacity) {
        TokenBucket bucket = bucketCache.getIfPresent(cacheKey);
        if (bucket == null) {
            bucket = new TokenBucket(capacity);
            bucketCache.put(cacheKey, bucket);
        } else {
            bucket.updateCapacity(capacity);
        }
        return bucket;
    }

    private double getLimit(String key) {
        SysConfig config = sysConfigMapper.selectByKey(key);
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            try {
                return Double.parseDouble(config.getConfigValue().trim());
            } catch (NumberFormatException ignored) {}
        }
        return switch (key) {
            case "ai.rate.task_split" -> defaultTaskSplit;
            case "ai.rate.risk_analyze" -> defaultRiskAnalyze;
            default -> defaultTaskSplit;
        };
    }

    /**
     * 简单令牌桶：按分钟维度限制，允许突发使用完整额度。
     */
    private static class TokenBucket {
        private double capacity;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(double capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized void updateCapacity(double capacity) {
            this.capacity = capacity;
            if (tokens > capacity) {
                tokens = capacity;
            }
        }

        synchronized boolean tryAcquire() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedMinutes = (now - lastRefillNanos) / (60.0 * 1_000_000_000.0);
            double tokensToAdd = elapsedMinutes * capacity;
            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillNanos = now;
            }
        }
    }
}
