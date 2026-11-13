package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class NotificationWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Map<Integer, CopyOnWriteArraySet<Session>> USER_SESSIONS = new ConcurrentHashMap<>();

    public void register(Integer userId, Session session) {
        USER_SESSIONS.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("用户 {} WebSocket 连接建立，当前连接数 {}", userId, USER_SESSIONS.get(userId).size());
    }

    public void unregister(Integer userId, Session session) {
        CopyOnWriteArraySet<Session> sessions = USER_SESSIONS.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                USER_SESSIONS.remove(userId);
            }
        }
        log.info("用户 {} WebSocket 连接关闭", userId);
    }

    public void sendToUser(Integer userId, Object message) {
        CopyOnWriteArraySet<Session> sessions = USER_SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String text = MAPPER.writeValueAsString(message);
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(text);
                    } catch (IOException e) {
                        log.warn("WebSocket 推送失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket 消息序列化失败，尝试发送简化消息", e);
            sendSimpleFallback(sessions);
        }
    }

    private void sendSimpleFallback(CopyOnWriteArraySet<Session> sessions) {
        try {
            String text = MAPPER.writeValueAsString(Map.of("type", "NOTIFICATION", "message", "您有一条新通知"));
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(text);
                    } catch (IOException e) {
                        log.warn("WebSocket 简化消息推送失败", e);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("WebSocket 简化消息序列化也失败", ex);
        }
    }

    public void sendToUsers(java.util.Collection<Integer> userIds, Object message) {
        for (Integer userId : userIds) {
            sendToUser(userId, message);
        }
    }

    public void sendStatsUpdate(Integer userId) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "STATS_UPDATE");
        sendToUser(userId, payload);
    }

    public void sendStatsUpdateToUsers(java.util.Collection<Integer> userIds) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "STATS_UPDATE");
        for (Integer userId : userIds) {
            sendToUser(userId, payload);
        }
    }

    public void sendLogUpdate(Integer projectId, java.util.Collection<Integer> userIds) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "LOG_UPDATE");
        payload.put("projectId", projectId);
        for (Integer userId : userIds) {
            sendToUser(userId, payload);
        }
    }
}
