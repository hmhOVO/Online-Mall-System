package com.example.controller;

import com.example.common.Result;
import com.example.entity.LoginLog;
import com.example.entity.User;
import com.example.exception.CustomerException;
import com.example.service.LoginAttemptService;
import com.example.service.LoginLogService;
import com.example.service.NotificationService;
import com.example.service.TokenBlacklistService;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final int LOGIN_FAIL_WARNING_THRESHOLD = 5;
    private static final Map<String, AtomicInteger> loginFailCounter = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> lastWarningTime = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;
    @Resource
    private TokenBlacklistService tokenBlacklistService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private NotificationService notificationService;
    @Resource
    private LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        Boolean rememberMe = (Boolean) params.get("rememberMe");
        String ip = getIp(getRequest());

        if (username == null || username.isBlank()) {
            throw new CustomerException("用户名不能为空");
        }

        if (loginAttemptService.isLocked(username)) {
            long remainingMinutes = loginAttemptService.getLockRemainingMinutes(username);
            recordLoginLog(null, username, 0, "账号已被锁定");
            throw new CustomerException("账号已被锁定，请 " + remainingMinutes + " 分钟后再试");
        }

        try {
            User user = userService.login(username, password, rememberMe != null && rememberMe);
            recordLoginLog(user.getId(), user.getUsername(), 1, null);
            loginAttemptService.loginSucceeded(username);
            loginFailCounter.remove(username + ":" + ip);
            return Result.success(user);
        } catch (CustomerException e) {
            loginAttemptService.loginFailed(username);
            int failCount = loginAttemptService.getFailCount(username);
            recordLoginLog(null, username, 0, e.getMessage());

            if (loginAttemptService.isLocked(username)) {
                long remainingMinutes = loginAttemptService.getLockRemainingMinutes(username);
                try {
                    String title = "账号锁定通知";
                    String content = "用户「" + username + "」在 IP " + ip + " 处连续登录失败 " + failCount + " 次，账号已被锁定 " + remainingMinutes + " 分钟。";
                    notificationService.sendSystemNotificationToAdmins(
                            "LOGIN_LOCKED", title, content, "/admin/user"
                    );
                } catch (Exception ex) {
                }
                throw new CustomerException("连续登录失败次数过多，账号已被锁定 " + remainingMinutes + " 分钟");
            }

            if (username != null && !username.isBlank() && ip != null) {
                String key = username + ":" + ip;
                AtomicInteger counter = loginFailCounter.computeIfAbsent(key, k -> new AtomicInteger(0));
                int warnCount = counter.incrementAndGet();
                if (warnCount >= LOGIN_FAIL_WARNING_THRESHOLD) {
                    LocalDateTime lastWarn = lastWarningTime.get(key);
                    if (lastWarn == null || lastWarn.plusMinutes(10).isBefore(LocalDateTime.now())) {
                        try {
                            String title = "登录告警：多次登录失败";
                            String content = "用户「" + username + "」在 IP " + ip + " 处连续登录失败 " + warnCount + " 次，请关注是否存在异常登录。";
                            notificationService.sendSystemNotificationToAdmins(
                                    "LOGIN_WARNING", title, content, "/admin/login-log"
                            );
                            lastWarningTime.put(key, LocalDateTime.now());
                        } catch (Exception ex) {
                        }
                    }
                }
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token != null) {
            tokenBlacklistService.addToBlacklist(token);
        }
        return Result.success();
    }

    private void recordLoginLog(Integer userId, String username, int status, String failReason) {
        try {
            HttpServletRequest request = getRequest();
            LoginLog log = new LoginLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setLoginType("PASSWORD");
            log.setIp(getIp(request));
            log.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
            log.setStatus(status);
            log.setFailReason(failReason);
            log.setLoginTime(LocalDateTime.now());
            loginLogService.save(log);
        } catch (Exception e) {
            // 登录日志记录失败不应影响登录流程
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
