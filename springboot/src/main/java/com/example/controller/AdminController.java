package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.CurrentUserContext;
import com.example.annotation.OperationLog;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.service.LoginAttemptService;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private UserService userService;
    @Resource
    private LoginAttemptService loginAttemptService;

    @RequirePermission("user.view")
    @GetMapping("/users")
    public Result users(@RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize,
                        @RequestParam(required = false) String username,
                        @RequestParam(required = false) String role,
                        @RequestParam(required = false) String status) {
        Page<User> page = userService.selectUserPage(pageNum, pageSize, username, role, status);
        return Result.success(page);
    }

    @RequirePermission("user.view")
    @GetMapping("/user/{id:\\d+}/lock-status")
    public Result getUserLockStatus(@PathVariable Integer id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new CustomerException("用户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("locked", loginAttemptService.isLocked(user.getUsername()));
        result.put("failCount", loginAttemptService.getFailCount(user.getUsername()));
        result.put("lockRemainingMinutes", loginAttemptService.getLockRemainingMinutes(user.getUsername()));
        result.put("maxAttempts", loginAttemptService.getMaxAttempts());
        result.put("lockDurationMinutes", loginAttemptService.getLockDurationMinutes());
        return Result.success(result);
    }

    @OperationLog("管理员解锁用户")
    @RequirePermission("user.edit")
    @PutMapping("/user/{id:\\d+}/unlock")
    public Result unlockUser(@PathVariable Integer id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new CustomerException("用户不存在");
        }
        loginAttemptService.unlockUser(user.getUsername());
        return Result.success();
    }

    @OperationLog("管理员批量解锁用户")
    @RequirePermission("user.edit")
    @PutMapping("/users/batch/unlock")
    public Result batchUnlockUsers(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomerException("请选择要解锁的用户");
        }
        List<User> users = userService.listByIds(ids);
        for (User user : users) {
            loginAttemptService.unlockUser(user.getUsername());
        }
        return Result.success();
    }

    @OperationLog("管理员新增用户")
    @RequirePermission("user.create")
    @PostMapping("/user")
    public Result addUser(@RequestBody User user) {
        userService.addByAdmin(user);
        return Result.success();
    }

    @OperationLog("批量导入学生")
    @RequirePermission("user.create")
    @PostMapping("/user/import")
    public Result importStudents(@RequestParam("file") MultipartFile file,
                                 @RequestParam("classId") Integer classId) {
        return Result.success(userService.importStudents(classId, file));
    }

    @OperationLog("批量导入教师")
    @RequirePermission("user.create")
    @PostMapping("/user/import-teachers")
    public Result importTeachers(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.importTeachers(file));
    }

    @OperationLog("管理员变更用户状态")
    @RequirePermission("user.edit")
    @PutMapping("/user/{id:\\d+}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestBody User user) {
        userService.updateStatus(id, user.getStatus());
        return Result.success();
    }

    @OperationLog("管理员批量变更用户状态")
    @RequirePermission("user.edit")
    @PutMapping("/users/batch/status")
    public Result batchUpdateStatus(@RequestParam String status, @RequestBody List<Integer> ids) {
        userService.batchUpdateStatus(ids, status);
        return Result.success();
    }

    @OperationLog("管理员重置密码")
    @RequirePermission("user.edit")
    @PutMapping("/user/{id:\\d+}/reset-password")
    public Result resetPassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String confirmPassword = body.get("confirmPassword");
        userService.resetPassword(id, confirmPassword);
        return Result.success();
    }

    @OperationLog("管理员批量重置密码")
    @RequirePermission("user.edit")
    @PutMapping("/users/batch/reset-password")
    public Result batchResetPassword(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String confirmPassword = (String) body.get("confirmPassword");
        userService.batchResetPassword(ids, confirmPassword);
        return Result.success();
    }

    @OperationLog("管理员删除用户")
    @RequirePermission("user.delete")
    @DeleteMapping("/user/{id:\\d+}")
    public Result deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @OperationLog("管理员批量删除用户")
    @RequirePermission("user.delete")
    @DeleteMapping("/users/batch")
    public Result batchDeleteUsers(@RequestBody List<Integer> ids) {
        userService.batchDeleteUsers(ids);
        return Result.success();
    }
}
