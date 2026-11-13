package com.example.controller;

import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/personal")
    public Result personal() {
        return Result.success(dashboardService.personal());
    }

    @GetMapping("/teacher")
    public Result teacher() {
        return Result.success(dashboardService.teacher());
    }

    @GetMapping("/project/{projectId}")
    public Result project(@PathVariable Integer projectId) {
        return Result.success(dashboardService.project(projectId));
    }

    @GetMapping("/ai/statistics")
    @RequirePermission("log.statistics")
    public Result aiStatistics() {
        return Result.success(dashboardService.aiStatistics());
    }

    @GetMapping("/admin")
    @RequirePermission("dashboard.view")
    public Result admin() {
        return Result.success(dashboardService.admin());
    }

    @GetMapping("/member-load")
    public Result memberLoad() {
        return Result.success(dashboardService.memberLoad());
    }

    @GetMapping("/admin/business")
    @RequirePermission("dashboard.view")
    public Result adminBusiness() {
        return Result.success(dashboardService.adminBusinessOverview());
    }
}
