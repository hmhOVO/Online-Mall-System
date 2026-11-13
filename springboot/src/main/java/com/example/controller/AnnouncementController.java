package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.entity.SysAnnouncement;
import com.example.service.SysAnnouncementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    private final SysAnnouncementService announcementService;

    public AnnouncementController(SysAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/published")
    public Result publishedList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SysAnnouncement> page = announcementService.listPublished(pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer id) {
        return Result.success(announcementService.getDetail(id));
    }
}
