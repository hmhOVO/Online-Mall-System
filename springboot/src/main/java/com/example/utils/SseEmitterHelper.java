package com.example.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 流式输出统一工具类，支持逐字发送、错误包装、中断检测。
 */
public final class SseEmitterHelper {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterHelper.class);

    private SseEmitterHelper() {}

    public static SseEmitter create() {
        return new SseEmitter(0L);
    }

    public static boolean send(SseEmitter emitter, String data) {
        if (emitter == null) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().data(data));
            return true;
        } catch (Exception e) {
            log.warn("SSE 发送失败，可能客户端已断开", e.getMessage());
            complete(emitter);
            return false;
        }
    }

    public static boolean sendError(SseEmitter emitter, String message) {
        return send(emitter, "[ERROR]" + message);
    }

    public static void complete(SseEmitter emitter) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}
