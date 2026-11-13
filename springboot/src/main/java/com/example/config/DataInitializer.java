package com.example.config;

import com.example.entity.Permission;
import com.example.mapper.PermissionMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Resource
    private PermissionMapper permissionMapper;

    private static final List<String> REQUIRED_PERMISSIONS = Arrays.asList(
            "user.create", "user.edit", "user.delete", "user.view",
            "project.create", "project.edit", "project.delete", "project.view", "project.transfer",
            "task.create", "task.edit", "task.delete", "task.view", "task.review", "task.comment",
            "ai.task_split", "ai.risk_analyze",
            "config.system",
            "log.ai", "log.operation", "log.login", "log.statistics",
            "data.export",
            "data.export.users", "data.export.projects", "data.export.tasks",
            "dashboard.view"
    );

    @Override
    public void run(String... args) {
        log.info("开始校验权限数据完整性...");

        List<Permission> allPermissions = permissionMapper.selectList(null);
        List<String> existingKeys = allPermissions.stream()
                .map(Permission::getPermissionKey)
                .collect(Collectors.toList());

        List<String> missingPermissions = REQUIRED_PERMISSIONS.stream()
                .filter(key -> !existingKeys.contains(key))
                .collect(Collectors.toList());

        if (missingPermissions.isEmpty()) {
            log.info("权限数据校验通过，共 {} 条权限", REQUIRED_PERMISSIONS.size());
        } else {
            log.error("权限数据不完整！缺少 {} 条权限：{}", missingPermissions.size(), missingPermissions);
            log.error("请执行 init.sql 初始化完整权限数据后再启动系统");
        }

        log.info("权限数据校验完成");
    }
}
