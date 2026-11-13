package com.example.controller;

import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.ProjectMember;
import com.example.service.MemberService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @RequirePermission("project.view")
    @GetMapping("/project-members")
    public Result getProjectMembers(@RequestParam Integer projectId) {
        List<ProjectMember> members = memberService.getProjectMembers(projectId);
        return Result.success(members);
    }

    @RequirePermission("project.view")
    @GetMapping("/my-projects-members")
    public Result getMyProjectsMembers() {
        List<ProjectMember> members = memberService.getMyProjectsMembers();
        return Result.success(members);
    }

    @RequirePermission("project.view")
    @GetMapping("/summary")
    public Result getMemberSummary() {
        List<Map<String, Object>> members = memberService.getMemberSummary();
        return Result.success(members);
    }

    @RequirePermission("project.view")
    @GetMapping("/{memberUserId}/tasks")
    public Result getMemberAllTasks(@PathVariable Integer memberUserId) {
        List<Map<String, Object>> tasks = memberService.getMemberAllTasks(memberUserId);
        return Result.success(tasks);
    }
}