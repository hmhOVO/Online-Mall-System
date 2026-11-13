package com.example.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(0);

    public String register(SseEmitter emitter, Integer userId) {
        String key = "sse-" + userId + "-" + System.currentTimeMillis() + "-" + counter.incrementAndGet();
        emitterMap.put(key, emitter);
        
        emitter.onCompletion(() -> {
            emitterMap.remove(key);
            log.debug("SSE 连接完成，已移除: {}", key);
        });
        
        emitter.onTimeout(() -> {
            emitterMap.remove(key);
            log.debug("SSE 连接超时，已移除: {}", key);
        });
        
        emitter.onError(e -> {
            emitterMap.remove(key);
            log.debug("SSE 连接错误，已移除: {}", key);
        });
        
        return key;
    }

    public boolean interrupt(String key) {
        SseEmitter emitter = emitterMap.remove(key);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("用户主动中断 AI 生成: {}", key);
                return true;
            } catch (Exception e) {
                log.warn("中断 SSE 连接失败: {}", e.getMessage());
            }
        }
        return false;
    }

    public void interruptByUserId(Integer userId) {
        for (Map.Entry<String, SseEmitter> entry : emitterMap.entrySet()) {
            if (entry.getKey().contains("-" + userId + "-")) {
                interrupt(entry.getKey());
            }
        }
    }

    public int getActiveCount() {
        return emitterMap.size();
    }
}