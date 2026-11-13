package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.TeachingClass;
import com.example.service.TeachingClassService;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeachingClassController {

    private final TeachingClassService teachingClassService;

    public TeachingClassController(TeachingClassService teachingClassService) {
        this.teachingClassService = teachingClassService;
    }

    @GetMapping("/course/list")
    public Result listAll(@RequestParam(required = false) Integer teacherId) {
        return Result.success(teachingClassService.listAll(teacherId));
    }

    /**
     * 教师端「我的课程」：本人授课课程 + 项目数/学生数。
     */
    @GetMapping("/course/my")
    public Result myCourses() {
        return Result.success(teachingClassService.listForCurrentTeacher());
    }

    @RequirePermission("course.manage")
    @GetMapping("/admin/course/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer classId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String term,
                       @RequestParam(required = false) Integer teacherId) {
        return Result.success(teachingClassService.list(pageNum, pageSize, keyword, classId, status, term, teacherId));
    }

    @OperationLog("新增课程")
    @RequirePermission("course.manage")
    @PostMapping("/admin/course")
    public Result create(@RequestBody TeachingClass teachingClass) {
        return Result.success(teachingClassService.create(teachingClass).getId());
    }

    @OperationLog("编辑课程")
    @RequirePermission("course.manage")
    @PutMapping("/admin/course/{id}")
    public Result update(@PathVariable Integer id, @RequestBody TeachingClass teachingClass) {
        teachingClassService.update(id, teachingClass);
        return Result.success();
    }

    @OperationLog("删除课程")
    @RequirePermission("course.manage")
    @DeleteMapping("/admin/course/{id}")
    public Result delete(@PathVariable Integer id) {
        teachingClassService.delete(id);
        return Result.success();
    }
}
