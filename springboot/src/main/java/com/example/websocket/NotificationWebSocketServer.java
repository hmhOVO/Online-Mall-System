package com.example.websocket;

import cn.hutool.core.util.StrUtil;
import com.example.common.SpringContextHolder;
import com.example.service.NotificationWebSocketService;
import com.example.service.TokenBlacklistService;
import com.example.utils.TokenUtils;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@ServerEndpoint("/ws/notification")
public class NotificationWebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketServer.class);

    private Integer userId;

    @OnOpen
    public void onOpen(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        List<String> tokenList = params.get("token");
        if (tokenList == null || tokenList.isEmpty() || StrUtil.isBlank(tokenList.get(0))) {
            close(session, "缺少 token");
            return;
        }
        String token = tokenList.get(0);
        TokenBlacklistService blacklistService = SpringContextHolder.getBean(TokenBlacklistService.class);
        if (blacklistService.isBlacklisted(token) || !TokenUtils.verify(token)) {
            close(session, "token 无效或已失效");
            return;
        }
        try {
            String audience = TokenUtils.getAudience(token);
            String[] split = audience.split("-");
            this.userId = Integer.valueOf(split[0]);
        } catch (Exception e) {
            close(session, "token 解析失败");
            return;
        }
        SpringContextHolder.getBean(NotificationWebSocketService.class).register(userId, session);
    }

    @OnClose
    public void onClose(Session session) {
        if (userId != null) {
            SpringContextHolder.getBean(NotificationWebSocketService.class).unregister(userId, session);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.warn("WebSocket 异常", error);
        if (userId != null) {
            SpringContextHolder.getBean(NotificationWebSocketService.class).unregister(userId, session);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 暂不需要处理客户端上行消息
    }

    private void close(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException e) {
            log.warn("关闭 WebSocket 失败", e);
        }
    }
}
