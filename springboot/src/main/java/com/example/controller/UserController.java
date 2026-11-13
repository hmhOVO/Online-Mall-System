package com.example.controller;

import com.example.annotation.OperationLog;
import com.example.common.Result;
import com.example.entity.User;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/profile")
    public Result profile() {
        return Result.success(userService.getCurrentProfile());
    }

    @OperationLog("更新个人资料")
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody User user) {
        userService.updateProfile(user);
        return Result.success();
    }

    @OperationLog("上传头像")
    @PostMapping("/avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(file));
    }

    @OperationLog("修改密码")
    @PutMapping("/password")
    public Result updatePassword(@RequestBody Map<String, String> params) {
        userService.updatePassword(params.get("oldPassword"), params.get("newPassword"));
        return Result.success();
    }

    @GetMapping("/search")
    public Result searchEnabledUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "20") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String role) {
        return Result.success(userService.searchEnabledUsers(pageNum, pageSize, keyword, role));
    }

    @GetMapping("/teachers")
    public Result listTeachers() {
        return Result.success(userService.listTeachers());
    }

}
