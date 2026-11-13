package com.example.controller;

import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.service.LoginLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/login-logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping("/stats")
    @RequirePermission("log.login")
    public Result stats() {
        return Result.success(loginLogService.getStats());
    }

    @GetMapping
    @RequirePermission("log.login")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(loginLogService.query(pageNum, pageSize, username, status, startDate, endDate));
    }
}
