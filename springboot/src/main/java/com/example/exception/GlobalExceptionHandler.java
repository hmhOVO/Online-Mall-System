package com.example.exception;

import com.example.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice("com.example")
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常");
    }

    @ExceptionHandler(CustomerException.class)
    @ResponseBody
    public Result customerError(CustomerException e) {
        log.error("业务错误: code={}, message={}", e.getCode(), e.getMeg());
        return Result.error(e.getCode(), e.getMeg());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public Result duplicateKeyError(DuplicateKeyException e) {
        log.warn("数据库唯一键冲突", e.getMessage());
        String msg = e.getMessage();
        if (msg != null && msg.contains("sys_user.uk_username_deleted")) {
            return Result.error("用户名已存在");
        }
        return Result.error("数据已存在，请勿重复提交");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseBody
    public Result authenticationError(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        if (e instanceof BadCredentialsException) {
            return Result.error("401", "用户名或密码错误");
        }
        if (e instanceof UsernameNotFoundException) {
            return Result.error("401", "用户不存在");
        }
        return Result.error("401", "认证失败，请重新登录");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public Result illegalArgumentError(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return Result.error("参数错误: " + e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public Result nullPointerError(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.error("系统异常");
    }
}
