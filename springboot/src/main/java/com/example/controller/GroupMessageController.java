package com.example.controller;

import com.example.common.Result;
import com.example.service.GroupMessageService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class GroupMessageController {

    private final GroupMessageService groupMessageService;

    public GroupMessageController(GroupMessageService groupMessageService) {
        this.groupMessageService = groupMessageService;
    }

    @GetMapping("/group/{groupId}/messages")
    public Result list(@PathVariable Integer groupId) {
        return Result.success(groupMessageService.list(groupId));
    }

    @PostMapping("/group/{groupId}/messages")
    public Result send(@PathVariable Integer groupId, @RequestBody Map<String, String> body) {
        return Result.success(groupMessageService.send(groupId, body.get("content")));
    }
}
