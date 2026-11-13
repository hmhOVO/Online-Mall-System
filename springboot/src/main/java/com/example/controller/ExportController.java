package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/admin/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/users")
    @OperationLog("导出用户数据")
    @RequirePermission("data.export.users")
    public void exportUsers(@RequestParam(required = false) String role,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String keyword,
                            HttpServletResponse response) throws IOException {
        exportService.exportUsers(role, status, keyword, response);
    }

    @GetMapping("/projects")
    @OperationLog("导出项目数据")
    @RequirePermission("data.export.projects")
    public void exportProjects(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String keyword,
                               HttpServletResponse response) throws IOException {
        exportService.exportProjects(status, keyword, response);
    }

    @GetMapping("/tasks")
    @OperationLog("导出任务数据")
    @RequirePermission("data.export.tasks")
    public void exportTasks(@RequestParam(required = false) Integer projectId,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String priority,
                            @RequestParam(required = false) Integer assigneeId,
                            HttpServletResponse response) throws IOException {
        exportService.exportTasks(projectId, status, priority, assigneeId, response);
    }

    @GetMapping("/scores")
    @OperationLog("导出实训成绩")
    @RequirePermission("score.export")
    public void exportScores(@RequestParam(required = false) Integer projectId,
                             @RequestParam(required = false) String keyword,
                             HttpServletResponse response) throws IOException {
        exportService.exportScores(projectId, keyword, response);
    }
}
