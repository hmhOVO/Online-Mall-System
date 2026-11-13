package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.Major;
import com.example.service.MajorService;
import org.springframework.web.bind.annotation.*;

@RestController
public class MajorController {

    private final MajorService majorService;

    public MajorController(MajorService majorService) {
        this.majorService = majorService;
    }

    @GetMapping("/major/list")
    public Result listAll(@RequestParam(required = false) Integer schoolId) {
        return Result.success(majorService.listAll(schoolId));
    }

    @RequirePermission("major.manage")
    @GetMapping("/admin/major/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer schoolId,
                       @RequestParam(required = false) String status) {
        return Result.success(majorService.list(pageNum, pageSize, keyword, schoolId, status));
    }

    @OperationLog("新增专业")
    @RequirePermission("major.manage")
    @PostMapping("/admin/major")
    public Result create(@RequestBody Major major) {
        return Result.success(majorService.create(major).getId());
    }

    @OperationLog("编辑专业")
    @RequirePermission("major.manage")
    @PutMapping("/admin/major/{id}")
    public Result update(@PathVariable Integer id, @RequestBody Major major) {
        majorService.update(id, major);
        return Result.success();
    }

    @OperationLog("删除专业")
    @RequirePermission("major.manage")
    @DeleteMapping("/admin/major/{id}")
    public Result delete(@PathVariable Integer id) {
        majorService.delete(id);
        return Result.success();
    }
}