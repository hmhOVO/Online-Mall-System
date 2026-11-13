package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.entity.Notification;
import com.example.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String type) {
        Page<Notification> page = notificationService.list(pageNum, pageSize, type);
        Map<String, Object> map = new HashMap<>();
        map.put("records", page.getRecords());
        map.put("total", page.getTotal());
        map.put("unread", notificationService.countUnread());
        map.put("typeCounts", notificationService.countByType());
        return Result.success(map);
    }

    @PutMapping("/{id:\\d+}/read")
    public Result markRead(@PathVariable Integer id) {
        notificationService.markRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result markAllRead() {
        notificationService.markAllRead();
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result unreadCount() {
        return Result.success(notificationService.countUnread());
    }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        notificationService.deleteBatch(ids);
        return Result.success();
    }
}
