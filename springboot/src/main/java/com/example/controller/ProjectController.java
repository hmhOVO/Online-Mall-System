package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.dto.CopyProjectRequest;
import com.example.entity.OperationLog;
import com.example.entity.Project;
import com.example.service.ExportService;
import com.example.service.ProjectService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project")
public class ProjectController {

    private final ProjectService projectService;
    private final ExportService exportService;

    public ProjectController(ProjectService projectService, ExportService exportService) {
        this.projectService = projectService;
        this.exportService = exportService;
    }

    @com.example.annotation.OperationLog("创建项目")
    @RequirePermission("project.create")
    @PostMapping
    public Result create(@RequestBody Project project) {
        projectService.createProject(project);
        return Result.success(project.getId());
    }

    @com.example.annotation.OperationLog("编辑项目")
    @RequirePermission("project.edit")
    @PutMapping("/{id:\\d+}")
    public Result update(@PathVariable Integer id, @RequestBody Project project) {
        projectService.updateProject(id, project);
        return Result.success();
    }

    @com.example.annotation.OperationLog("复制项目")
    @RequirePermission("project.create")
    @PostMapping("/{id:\\d+}/copy")
    public Result copy(@PathVariable Integer id, @RequestBody CopyProjectRequest request) {
        return Result.success(projectService.copyProject(id, request));
    }

    @com.example.annotation.OperationLog("删除项目")
    @RequirePermission("project.delete")
    @DeleteMapping("/{id:\\d+}")
    public Result delete(@PathVariable Integer id) {
        projectService.deleteProject(id);
        return Result.success();
    }

    @com.example.annotation.OperationLog("归档项目")
    @RequirePermission("project.edit")
    @PutMapping("/{id:\\d+}/archive")
    public Result archive(@PathVariable Integer id, @RequestParam Boolean archived) {
        projectService.archiveProject(id, Boolean.TRUE.equals(archived));
        return Result.success();
    }

    @GetMapping("/list")
    public Result list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateBegin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateEnd,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) Integer courseId) {
        return Result.success(projectService.listProjects(pageNum, pageSize, name, status, startDateBegin, startDateEnd, archived, courseId));
    }

    @GetMapping("/recycle-bin/list")
    public Result recycleBinList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return Result.success(projectService.listDeletedProjects(pageNum, pageSize, name, status));
    }

    @com.example.annotation.OperationLog("恢复项目")
    @RequirePermission("project.edit")
    @PutMapping("/{id:\\d+}/restore")
    public Result restore(@PathVariable Integer id) {
        projectService.restoreProject(id);
        return Result.success();
    }

    @com.example.annotation.OperationLog("彻底删除项目")
    @RequirePermission("project.delete")
    @DeleteMapping("/{id:\\d+}/purge")
    public Result purge(@PathVariable Integer id) {
        projectService.purgeProject(id);
        return Result.success();
    }

    @com.example.annotation.OperationLog("清空项目回收站")
    @RequirePermission("project.delete")
    @DeleteMapping("/recycle-bin/clear")
    public Result clearRecycleBin() {
        return Result.success(projectService.purgeAllDeletedProjects());
    }

    @GetMapping("/{id:\\d+}")
    public Result detail(@PathVariable Integer id) {
        return Result.success(projectService.getProjectDetail(id));
    }

    @com.example.annotation.OperationLog("添加项目成员")
    @RequirePermission("project.edit")
    @PostMapping("/{id:\\d+}/members")
    public Result addMember(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        Integer userId = Integer.valueOf(params.get("userId").toString());
        projectService.addMember(id, userId);
        return Result.success();
    }

    @com.example.annotation.OperationLog("移除项目成员")
    @RequirePermission("project.edit")
    @DeleteMapping("/{id:\\d+}/members/{userId}")
    public Result removeMember(@PathVariable Integer id, @PathVariable Integer userId) {
        projectService.removeMember(id, userId);
        return Result.success();
    }

    @com.example.annotation.OperationLog("变更项目状态")
    @RequirePermission("project.edit")
    @PutMapping("/{id:\\d+}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        projectService.updateStatus(id, params.get("status"));
        return Result.success();
    }

    @com.example.annotation.OperationLog("项目结项")
    @RequirePermission("project.edit")
    @PostMapping("/{id:\\d+}/complete")
    public Result complete(@PathVariable Integer id) {
        projectService.completeProject(id);
        return Result.success();
    }

    @GetMapping("/{id:\\d+}/unfinished-task-count")
    public Result unfinishedTaskCount(@PathVariable Integer id) {
        return Result.success(projectService.countUnfinishedTasks(id));
    }

    @GetMapping("/{id:\\d+}/scores")
    public Result scores(@PathVariable Integer id) {
        return Result.success(projectService.getProjectScores(id));
    }

    @GetMapping("/{id:\\d+}/group-scores")
    public Result groupScores(@PathVariable Integer id) {
        return Result.success(projectService.getGroupScores(id));
    }

    @com.example.annotation.OperationLog("导出项目成绩")
    @GetMapping("/{id:\\d+}/scores/export")
    public void exportScores(@PathVariable Integer id, HttpServletResponse response) throws IOException {
        projectService.getProjectScores(id);
        exportService.exportScores(id, null, response);
    }

    @com.example.annotation.OperationLog("重启项目")
    @RequirePermission("project.edit")
    @PutMapping("/{id:\\d+}/restart")
    public Result restart(@PathVariable Integer id) {
        projectService.restartProject(id);
        return Result.success();
    }

    @com.example.annotation.OperationLog("变更指导教师")
    @RequirePermission("project.transfer")
    @PutMapping("/{id:\\d+}/transfer")
    public Result transfer(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        Integer newTeacherId = Integer.valueOf(params.get("newTeacherId"));
        projectService.transferProject(id, newTeacherId);
        return Result.success();
    }

    @com.example.annotation.OperationLog("指定组长")
    @RequirePermission("project.edit")
    @PutMapping("/{id:\\d+}/leader")
    public Result assignLeader(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        Integer studentId = Integer.valueOf(params.get("studentId"));
        projectService.assignGroupLeader(id, studentId);
        return Result.success();
    }

    @GetMapping("/{id:\\d+}/logs")
    public Result logs(@PathVariable Integer id) {
        return Result.success(projectService.listLogs(id));
    }
}
