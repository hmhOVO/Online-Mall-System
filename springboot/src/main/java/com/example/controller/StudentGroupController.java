package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.StudentGroup;
import com.example.service.StudentGroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    public StudentGroupController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @GetMapping("/project/{projectId}/groups")
    public Result list(@PathVariable Integer projectId) {
        return Result.success(studentGroupService.listByProject(projectId));
    }

    @GetMapping("/group/{groupId}/members")
    public Result members(@PathVariable Integer groupId) {
        return Result.success(studentGroupService.listMembers(groupId));
    }

    @OperationLog("创建实训分组")
    @RequirePermission("group.manage")
    @PostMapping("/group")
    public Result create(@RequestBody StudentGroup group) {
        return Result.success(studentGroupService.create(group).getId());
    }

    @OperationLog("编辑实训分组")
    @RequirePermission("group.manage")
    @PutMapping("/group/{id}")
    public Result update(@PathVariable Integer id, @RequestBody StudentGroup group) {
        studentGroupService.update(id, group);
        return Result.success();
    }

    @OperationLog("修改小组名称")
    @PutMapping("/group/{id}/name")
    public Result rename(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        studentGroupService.renameByMember(id, params.get("name"));
        return Result.success();
    }

    @OperationLog("删除实训分组")
    @RequirePermission("group.manage")
    @DeleteMapping("/group/{id}")
    public Result delete(@PathVariable Integer id) {
        studentGroupService.delete(id);
        return Result.success();
    }

    @OperationLog("添加分组成员")
    @RequirePermission("group.manage")
    @PostMapping("/group/{groupId}/member")
    public Result addMembers(@PathVariable Integer groupId, @RequestBody Map<String, List<Integer>> params) {
        studentGroupService.addMembers(groupId, params.get("studentIds"));
        return Result.success();
    }

    @OperationLog("移除分组成员")
    @RequirePermission("group.manage")
    @DeleteMapping("/group/{groupId}/member/{studentId}")
    public Result removeMember(@PathVariable Integer groupId, @PathVariable Integer studentId) {
        studentGroupService.removeMember(groupId, studentId);
        return Result.success();
    }

    @OperationLog("学生加入小组")
    @PostMapping("/group/{groupId}/join")
    public Result join(@PathVariable Integer groupId) {
        studentGroupService.studentJoin(groupId);
        return Result.success();
    }

    @OperationLog("学生退出小组")
    @DeleteMapping("/group/{groupId}/leave")
    public Result leave(@PathVariable Integer groupId) {
        studentGroupService.studentLeave(groupId);
        return Result.success();
    }

    @OperationLog("批量创建实训分组")
    @RequirePermission("group.manage")
    @PostMapping("/project/{projectId}/groups/batch")
    public Result batchCreate(@PathVariable Integer projectId, @RequestBody Map<String, Object> params) {
        Integer count = params.get("count") == null ? null : Integer.valueOf(params.get("count").toString());
        Integer maxMembers = params.get("maxMembers") == null ? null : Integer.valueOf(params.get("maxMembers").toString());
        Boolean joinEnabled = params.get("joinEnabled") == null ? null : Boolean.valueOf(params.get("joinEnabled").toString());
        return Result.success(studentGroupService.batchCreate(projectId, count, maxMembers, joinEnabled));
    }
}