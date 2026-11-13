package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.ClassInfo;
import com.example.service.ClassInfoService;
import org.springframework.web.bind.annotation.*;

@RestController
public class ClassInfoController {

    private final ClassInfoService classInfoService;

    public ClassInfoController(ClassInfoService classInfoService) {
        this.classInfoService = classInfoService;
    }

    @GetMapping("/class/list")
    public Result listAll(@RequestParam(required = false) Integer majorId) {
        return Result.success(classInfoService.listAll(majorId));
    }

    @RequirePermission("class.manage")
    @GetMapping("/admin/class/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer majorId,
                       @RequestParam(required = false) String status) {
        return Result.success(classInfoService.list(pageNum, pageSize, keyword, majorId, status));
    }

    @OperationLog("新增班级")
    @RequirePermission("class.manage")
    @PostMapping("/admin/class")
    public Result create(@RequestBody ClassInfo classInfo) {
        return Result.success(classInfoService.create(classInfo).getId());
    }

    @OperationLog("编辑班级")
    @RequirePermission("class.manage")
    @PutMapping("/admin/class/{id}")
    public Result update(@PathVariable Integer id, @RequestBody ClassInfo classInfo) {
        classInfoService.update(id, classInfo);
        return Result.success();
    }

    @OperationLog("删除班级")
    @RequirePermission("class.manage")
    @DeleteMapping("/admin/class/{id}")
    public Result delete(@PathVariable Integer id) {
        classInfoService.delete(id);
        return Result.success();
    }
}