package com.example.interceptor;

import com.example.common.CurrentUserContext;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = CurrentUserContext.getRole();
        if (RoleEnum.ADMIN.name().equals(role)) {
            return true;
        }
        if (RoleEnum.TEACHER.name().equals(role)
                && "GET".equalsIgnoreCase(request.getMethod())
                && (request.getRequestURI().endsWith("/admin/users")
                || request.getRequestURI().startsWith("/admin/export/"))) {
            return true;
        }
        throw new CustomerException("403", "无系统管理员权限");
    }
}
