package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.CurrentUserContext;
import com.example.entity.Notification;
import com.example.entity.User;
import com.example.mapper.NotificationMapper;
import com.example.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper notificationMapper;
    private final NotificationWebSocketService webSocketService;
    private final UserMapper userMapper;

    public NotificationService(NotificationMapper notificationMapper,
                               NotificationWebSocketService webSocketService,
                               UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.webSocketService = webSocketService;
        this.userMapper = userMapper;
    }

    public Page<Notification> list(Integer pageNum, Integer pageSize, String type) {
        User current = CurrentUserContext.get();
        LambdaQueryWrapper<Notification> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Notification::getUserId, current.getId())
                .eq(Notification::getDeleted, 0);
        if (type != null && !type.isBlank()) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public Map<String, Integer> countByType() {
        User current = CurrentUserContext.get();
        List<Map<String, Object>> results = notificationMapper.countByType(current.getId());
        Map<String, Integer> counts = new HashMap<>();
        counts.put("TASK", 0);
        counts.put("AUDIT", 0);
        counts.put("PROJECT", 0);
        counts.put("SYSTEM", 0);
        for (Map<String, Object> result : results) {
            String type = (String) result.get("type");
            Long count = (Long) result.get("count");
            if (type != null) {
                if (type.startsWith("TASK_") || type.equals("ASSIGN") || type.equals("OVERDUE") || type.equals("TASK_CLAIM")) {
                    counts.merge("TASK", count.intValue(), Integer::sum);
                } else if (type.equals("REJECT") || type.equals("MENTION")) {
                    counts.merge("AUDIT", count.intValue(), Integer::sum);
                } else if (type.equals("MEMBER") || type.equals("GROUP_INVITE")) {
                    counts.merge("PROJECT", count.intValue(), Integer::sum);
                } else if (type.equals("AI_RISK") || type.equals("LOGIN_WARNING")) {
                    counts.merge("SYSTEM", count.intValue(), Integer::sum);
                }
            }
        }
        return counts;
    }

    public long countUnread() {
        User current = CurrentUserContext.get();
        return notificationMapper.countUnread(current.getId());
    }

    @Transactional
    public void markRead(Integer id) {
        User current = CurrentUserContext.get();
        LambdaUpdateWrapper<Notification> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Notification::getId, id)
                .eq(Notification::getUserId, current.getId())
                .set(Notification::getIsRead, true);
        notificationMapper.update(null, wrapper);
    }

    @Transactional
    public void markAllRead() {
        User current = CurrentUserContext.get();
        notificationMapper.markAllRead(current.getId());
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        User current = CurrentUserContext.get();
        LambdaUpdateWrapper<Notification> wrapper = Wrappers.lambdaUpdate();
        wrapper.in(Notification::getId, ids)
                .eq(Notification::getUserId, current.getId())
                .set(Notification::getDeleted, 1);
        notificationMapper.update(null, wrapper);
    }

    @Transactional
    public void send(Integer userId, String type, String title, String content, String targetUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetUrl(targetUrl);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        webSocketService.sendToUser(userId, toPayload(notification));
    }

    public void sendBatch(Collection<Integer> userIds, String type, String title, String content, String targetUrl) {
        for (Integer userId : userIds) {
            send(userId, type, title, content, targetUrl);
        }
    }

    private Map<String, Object> toPayload(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("type", notification.getType());
        map.put("title", notification.getTitle());
        map.put("content", notification.getContent());
        map.put("targetUrl", notification.getTargetUrl());
        map.put("isRead", notification.getIsRead());
        map.put("createdAt", notification.getCreatedAt());
        return map;
    }

    public void sendSystemNotificationToAdmins(String type, String title, String content, String targetUrl) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getRole, "ADMIN")
                .eq(User::getStatus, "ENABLED")
                .eq(User::getDeleted, 0)
                .select(User::getId);
        List<Integer> adminIds = userMapper.selectList(wrapper).stream()
                .map(User::getId)
                .toList();
        if (!adminIds.isEmpty()) {
            sendBatch(adminIds, type, title, content, targetUrl);
        }
    }
}
