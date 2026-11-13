package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.dto.AiSplitConfirmRequest;
import com.example.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @RequirePermission("ai.task_split")
    @PostMapping("/task/split/preview")
    public Result previewSplitTask(@RequestBody Map<String, Object> params) {
        Integer projectId = Integer.valueOf(params.get("projectId").toString());
        String requirement = (String) params.get("requirement");
        Integer studentCount = params.get("studentCount") == null ? 3 : Integer.valueOf(params.get("studentCount").toString());
        String deadline = params.get("deadline") == null ? null : params.get("deadline").toString();
        return Result.success(aiService.previewSplitTasks(projectId, requirement, studentCount, deadline));
    }

    @RequirePermission("ai.task_split")
    @GetMapping(value = "/task/split/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSplitTask(
            @RequestParam Integer projectId,
            @RequestParam String requirement,
            @RequestParam(defaultValue = "3") Integer studentCount,
            @RequestParam(required = false) String deadline) {
        return aiService.streamSplitTasks(projectId, requirement, studentCount, deadline);
    }

    @com.example.annotation.OperationLog(value = "AI任务拆解确认", projectIdParam = "projectId")
    @RequirePermission("ai.task_split")
    @PostMapping("/task/split/confirm")
    public Result confirmSplitTask(@RequestBody AiSplitConfirmRequest request) {
        return Result.success(aiService.confirmSplitTasks(request.getProjectId(), request.getTasks()));
    }

    @RequirePermission("ai.risk_analyze")
    @GetMapping(value = "/risk/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeRisk(@RequestParam Integer projectId) {
        return aiService.analyzeRisk(projectId);
    }

    @GetMapping("/logs/stats")
    @RequirePermission("log.ai")
    public Result logStats() {
        return Result.success(aiService.getLogStats());
    }

    @GetMapping("/logs")
    @RequirePermission("log.ai")
    public Result logs(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) Integer userId,
                       @RequestParam(required = false) Integer projectId,
                       @RequestParam(required = false) String scene,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String aiType) {
        return Result.success(aiService.queryLogs(pageNum, pageSize, userId, projectId, scene, status, aiType));
    }

    @PostMapping("/interrupt")
    public Result interrupt() {
        aiService.interruptAi();
        return Result.success();
    }

}
