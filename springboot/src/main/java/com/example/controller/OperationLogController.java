package com.example.controller;

import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.service.OperationLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    @RequirePermission("log.operation")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) Integer userId,
                       @RequestParam(required = false) String operationType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(operationLogService.query(pageNum, pageSize, userId, operationType, startDate, endDate));
    }

    @GetMapping("/statistics")
    @RequirePermission("log.operation")
    public Result statistics() {
        return Result.success(operationLogService.getStatistics());
    }
}
