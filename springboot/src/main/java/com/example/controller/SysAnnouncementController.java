package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.SysAnnouncement;
import com.example.service.SysAnnouncementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/announcement")
public class SysAnnouncementController {

    private final SysAnnouncementService announcementService;

    public SysAnnouncementController(SysAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @com.example.annotation.OperationLog("查看公告列表")
    @RequirePermission("system.announcement")
    @GetMapping("/list")
    public Result list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        Page<SysAnnouncement> page = announcementService.listAnnouncements(pageNum, pageSize, status, type, keyword);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer id) {
        return Result.success(announcementService.getDetail(id));
    }

    @com.example.annotation.OperationLog("创建公告")
    @RequirePermission("system.announcement")
    @PostMapping
    public Result create(@RequestBody SysAnnouncement announcement) {
        announcementService.createAnnouncement(announcement);
        return Result.success(announcement.getId());
    }

    @com.example.annotation.OperationLog("编辑公告")
    @RequirePermission("system.announcement")
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody SysAnnouncement announcement) {
        announcementService.updateAnnouncement(id, announcement);
        return Result.success();
    }

    @com.example.annotation.OperationLog("删除公告")
    @RequirePermission("system.announcement")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    @com.example.annotation.OperationLog("发布公告")
    @RequirePermission("system.announcement")
    @PutMapping("/{id}/publish")
    public Result publish(@PathVariable Integer id) {
        announcementService.publishAnnouncement(id);
        return Result.success();
    }

    @com.example.annotation.OperationLog("下线公告")
    @RequirePermission("system.announcement")
    @PutMapping("/{id}/offline")
    public Result offline(@PathVariable Integer id) {
        announcementService.offlineAnnouncement(id);
        return Result.success();
    }

    @com.example.annotation.OperationLog("公告置顶/取消置顶")
    @RequirePermission("system.announcement")
    @PutMapping("/{id}/toggle-top")
    public Result toggleTop(@PathVariable Integer id) {
        announcementService.toggleTop(id);
        return Result.success();
    }
}
