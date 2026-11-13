package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.Permission;
import com.example.enums.RoleEnum;
import com.example.mapper.PermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService extends ServiceImpl<PermissionMapper, Permission> {

    // 系统管理员可见的权限集合：系统级全部 + 业务只读 + AI 功能 + 实训组织相关
    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            "user.view", "user.create", "user.edit", "user.delete",
            "config.system",
            "log.operation", "log.login", "log.ai", "log.statistics",
            "data.export",
            "data.export.users", "data.export.projects", "data.export.tasks",
            "dashboard.view",
            "system.announcement",
            // 业务级 - 只读
            "project.view",
            "task.view",
            // AI 功能
            "ai.task_split", "ai.risk_analyze",
            // 实训组织架构 / 分组 / 成绩
            "school.manage", "major.manage", "class.manage", "course.manage", "group.manage",
            "task.score_view", "score.export", "dashboard.teacher"
    );

    private final PermissionMapper permissionMapper;

    public PermissionService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<Permission> listAll() {
        LambdaQueryWrapper<Permission> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByAsc(Permission::getModule).orderByAsc(Permission::getPermissionName);
        return permissionMapper.selectList(wrapper);
    }

    public List<Permission> getPermissionsByRole(String role) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            return listAll().stream()
                    .filter(p -> ADMIN_PERMISSIONS.contains(p.getPermissionKey()))
                    .collect(Collectors.toList());
        }
        return permissionMapper.selectByRole(role);
    }

    public Set<String> getPermissionKeysByRole(String role) {
        return getPermissionsByRole(role).stream()
                .map(Permission::getPermissionKey)
                .collect(Collectors.toSet());
    }
}
