package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.Task;
import com.example.entity.TaskAttachment;
import com.example.service.TaskAttachmentService;
import com.example.service.TaskService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class TaskController {

    private final TaskService taskService;
    private final TaskAttachmentService taskAttachmentService;

    public TaskController(TaskService taskService, TaskAttachmentService taskAttachmentService) {
        this.taskService = taskService;
        this.taskAttachmentService = taskAttachmentService;
    }

    @OperationLog("创建任务")
    @RequirePermission("task.create")
    @PostMapping("/task")
    public Result create(@RequestBody Task task) {
        taskService.createTask(task);
        return Result.success();
    }

    @OperationLog("编辑任务")
    @RequirePermission("task.edit")
    @PutMapping("/task/{id:\\d+}")
    public Result update(@PathVariable Integer id, @RequestBody Task task) {
        taskService.updateTask(id, task);
        return Result.success();
    }

    @OperationLog("删除任务")
    @RequirePermission("task.delete")
    @DeleteMapping("/task/{id:\\d+}")
    public Result delete(@PathVariable Integer id) {
        taskService.deleteTask(id);
        return Result.success();
    }

    @GetMapping("/task/{id:\\d+}")
    public Result detail(@PathVariable Integer id) {
        return Result.success(taskService.getTaskDetail(id));
    }

    @GetMapping("/project/{projectId}/tasks")
    public Result list(@PathVariable Integer projectId,
                       @RequestParam(required = false) List<String> statuses,
                       @RequestParam(required = false) List<String> priorities,
                       @RequestParam(required = false) Integer assigneeId,
                       @RequestParam(required = false) Boolean onlyOverdue,
                       @RequestParam(required = false) String keyword) {
        return Result.success(taskService.listProjectTasks(projectId, statuses, priorities, assigneeId, onlyOverdue, keyword));
    }

    @GetMapping("/task/my-list")
    public Result myList(@RequestParam(required = false) List<String> statuses,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String relationType) {
        return Result.success(taskService.listMyTasks(statuses, keyword, relationType));
    }

    @GetMapping("/task/training-list")
    public Result trainingList(@RequestParam(required = false) List<Integer> projectIds,
                           @RequestParam(required = false) List<String> statuses,
                           @RequestParam(required = false) List<String> priorities,
                           @RequestParam(required = false) Integer assigneeId,
                           @RequestParam(required = false) String keyword) {
        return Result.success(taskService.listTrainingTasks(projectIds, statuses, priorities, assigneeId, keyword));
    }

    @GetMapping("/task/my-calendar")
    public Result myCalendar() {
        return Result.success(taskService.listMyTasks(null, null, null));
    }

    @OperationLog("变更任务状态")
    @RequirePermission("task.edit")
    @PutMapping("/task/{id:\\d+}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        taskService.updateStatus(id, params.get("status"));
        return Result.success();
    }

    @OperationLog("更新任务进度")
    @RequirePermission("task.edit")
    @PutMapping("/task/{id:\\d+}/progress")
    public Result updateProgress(@PathVariable Integer id, @RequestBody Map<String, Integer> params) {
        taskService.updateProgress(id, params.get("progress"));
        return Result.success();
    }

    @OperationLog("更新实际学时")
    @RequirePermission("task.edit")
    @PutMapping("/task/{id:\\d+}/actual-hours")
    public Result updateActualHours(@PathVariable Integer id, @RequestBody Map<String, Double> params) {
        taskService.updateActualHours(id, params.get("actualHours"));
        return Result.success();
    }

    @OperationLog("认领任务")
    @RequirePermission("task.edit")
    @PostMapping("/task/{id:\\d+}/claim")
    public Result claim(@PathVariable Integer id) {
        taskService.claimTask(id);
        return Result.success();
    }

    @OperationLog("提交任务审核")
    @RequirePermission("task.edit")
    @PostMapping("/task/{id:\\d+}/review")
    public Result submitReview(@PathVariable Integer id, @RequestBody(required = false) Map<String, Integer> params) {
        Integer reviewerId = params != null ? params.get("reviewerId") : null;
        taskService.submitReview(id, reviewerId);
        return Result.success();
    }

    @OperationLog("通过任务审核")
    @RequirePermission("task.review")
    @PutMapping("/task/{id:\\d+}/review/approve")
    public Result approveReview(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> params) {
        Double score = params != null && params.get("score") != null
                ? Double.valueOf(params.get("score").toString()) : null;
        taskService.approveReview(id, score);
        return Result.success();
    }

    @OperationLog("任务评分")
    @RequirePermission("task.review")
    @PutMapping("/task/{id:\\d+}/score")
    public Result score(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> params) {
        Double score = params != null && params.get("score") != null
                ? Double.valueOf(params.get("score").toString()) : null;
        taskService.scoreTask(id, score);
        return Result.success();
    }

    @OperationLog("驳回任务审核")
    @RequirePermission("task.review")
    @PutMapping("/task/{id:\\d+}/review/reject")
    public Result rejectReview(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        taskService.rejectReview(id, params.get("comment"));
        return Result.success();
    }

    @OperationLog("撤回任务审核")
    @RequirePermission("task.edit")
    @PutMapping("/task/{id:\\d+}/review/withdraw")
    public Result withdrawReview(@PathVariable Integer id) {
        taskService.withdrawReview(id);
        return Result.success();
    }

    @OperationLog("直接完成任务")
    @RequirePermission("task.edit")
    @PutMapping("/task/{id:\\d+}/complete")
    public Result completeDirectly(@PathVariable Integer id) {
        taskService.completeDirectly(id);
        return Result.success();
    }

    @RequirePermission("task.edit")
    @PostMapping("/task/{id:\\d+}/dependency/check")
    public Result checkDependencyCycle(@PathVariable Integer id, @RequestBody Map<String, List<Integer>> params) {
        boolean hasCycle = taskService.checkDependencyCycle(id, params.get("dependencyIds"));
        return Result.success(hasCycle);
    }

    @OperationLog("添加任务评论")
    @RequirePermission("task.comment")
    @PostMapping("/task/{id:\\d+}/comment")
    public Result addComment(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        Integer parentCommentId = params.get("parentCommentId") == null ? null : Integer.valueOf(params.get("parentCommentId").toString());
        taskService.addComment(id, parentCommentId, (String) params.get("content"));
        return Result.success();
    }

    @OperationLog("标记评论解决状态")
    @RequirePermission("task.comment")
    @PutMapping("/task/comment/{id:\\d+}/resolve")
    public Result resolveComment(@PathVariable Integer id, @RequestBody Map<String, Boolean> params) {
        taskService.resolveComment(id, params.get("resolved"));
        return Result.success();
    }

    @OperationLog("编辑任务评论")
    @RequirePermission("task.comment")
    @PutMapping("/task/comment/{id:\\d+}")
    public Result updateComment(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        taskService.updateComment(id, params.get("content"));
        return Result.success();
    }

    @OperationLog("删除任务评论")
    @RequirePermission("task.comment")
    @DeleteMapping("/task/comment/{id:\\d+}")
    public Result deleteComment(@PathVariable Integer id) {
        taskService.deleteComment(id);
        return Result.success();
    }

    @GetMapping("/task/{id:\\d+}/change-log")
    public Result getTaskChangeLog(@PathVariable Integer id) {
        return Result.success(taskService.getTaskChangeLogs(id));
    }

    @GetMapping("/task/overdue/list")
    public Result overdueList() {
        return Result.success(taskService.listOverdueTasks());
    }

    @GetMapping("/task/{id:\\d+}/attachments")
    public Result listAttachments(@PathVariable Integer id) {
        return Result.success(taskAttachmentService.listByTaskId(id));
    }

    @OperationLog("上传任务附件")
    @RequirePermission("task.edit")
    @PostMapping("/task/{id:\\d+}/attachment")
    public Result uploadAttachment(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return Result.success(taskAttachmentService.upload(id, file));
    }

    @OperationLog("删除任务附件")
    @RequirePermission("task.edit")
    @DeleteMapping("/task/{id:\\d+}/attachment/{attachmentId:\\d+}")
    public Result deleteAttachment(@PathVariable Integer id, @PathVariable Integer attachmentId) {
        taskAttachmentService.delete(id, attachmentId);
        return Result.success();
    }
}
