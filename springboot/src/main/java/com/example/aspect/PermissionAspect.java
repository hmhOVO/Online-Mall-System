package com.example.aspect;

import com.example.common.CurrentUserContext;
import com.example.common.ProjectRoleLevel;
import com.example.annotation.RequirePermission;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.service.PermissionService;
import com.example.service.ProjectService;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

@Aspect
@Component
public class PermissionAspect {

    @Resource
    private PermissionService permissionService;

    @Resource
    private ProjectService projectService;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        String role = current.getRole();
        String permission = requirePermission.value();
        Set<String> permissions = permissionService.getPermissionKeysByRole(role);
        if (permissions.contains(permission)) {
            return;
        }

        // 全局权限不足时，针对项目级权限检查项目角色
        if (isProjectScopedPermission(permission)) {
            Integer projectId = extractProjectId(joinPoint);
            if (projectId != null) {
                ProjectRoleLevel level = projectService.getUserProjectRole(projectId, current.getId());
                if (hasProjectPermission(level, permission)) {
                    return;
                }
            }
        }

        throw new CustomerException("无权限执行该操作");
    }

    private boolean isProjectScopedPermission(String permission) {
        return permission.startsWith("project.") || permission.startsWith("ai.") || permission.startsWith("task.");
    }

    private boolean hasProjectPermission(ProjectRoleLevel level, String permission) {
        if (level == null || level == ProjectRoleLevel.NONE) {
            return false;
        }
        // 项目负责人（组长）拥有项目内所有权限
        if (level == ProjectRoleLevel.OWNER) {
            return true;
        }
        return false;
    }

    private Integer extractProjectId(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 优先从参数名 projectId 或 id 提取
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String name = parameterNames[i];
                if (("projectId".equals(name) || "id".equals(name)) && args[i] instanceof Integer) {
                    return (Integer) args[i];
                }
            }
        }

        // 其次从请求参数 projectId 提取（适用于 /ai/risk/analyze?projectId=1）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String projectIdParam = attributes.getRequest().getParameter("projectId");
            if (projectIdParam != null && !projectIdParam.isBlank()) {
                try {
                    return Integer.valueOf(projectIdParam);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return null;
    }
}
