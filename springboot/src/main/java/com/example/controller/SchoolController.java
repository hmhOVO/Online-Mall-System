package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.School;
import com.example.service.SchoolService;
import org.springframework.web.bind.annotation.*;

@RestController
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping("/school/list")
    public Result listAll() {
        return Result.success(schoolService.listAll());
    }

    @RequirePermission("school.manage")
    @GetMapping("/admin/school/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status) {
        return Result.success(schoolService.list(pageNum, pageSize, keyword, status));
    }

    @OperationLog("新增学院")
    @RequirePermission("school.manage")
    @PostMapping("/admin/school")
    public Result create(@RequestBody School school) {
        return Result.success(schoolService.create(school).getId());
    }

    @OperationLog("编辑学院")
    @RequirePermission("school.manage")
    @PutMapping("/admin/school/{id}")
    public Result update(@PathVariable Integer id, @RequestBody School school) {
        schoolService.update(id, school);
        return Result.success();
    }

    @OperationLog("删除学院")
    @RequirePermission("school.manage")
    @DeleteMapping("/admin/school/{id}")
    public Result delete(@PathVariable Integer id) {
        schoolService.delete(id);
        return Result.success();
    }
}