package com.example.controller;

import com.example.common.CurrentUserContext;
import com.example.annotation.RequirePermission;
import com.example.common.Result;
import com.example.entity.SysConfig;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.service.SysConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SysConfigController {

    private final SysConfigService sysConfigService;

    public SysConfigController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @GetMapping("/config/public")
    public Result publicConfig() {
        Map<String, String> result = new HashMap<>();
        List<String> publicKeys = Arrays.asList("overdue.warning.days");
        for (String key : publicKeys) {
            SysConfig config = sysConfigService.getByKey(key);
            result.put(key, config == null ? "" : config.getConfigValue());
        }
        return Result.success(result);
    }

    @GetMapping("/admin/config")
    @RequirePermission("config.system")
    public Result list() {
        return Result.success(sysConfigService.listAll());
    }

    @PutMapping("/admin/config/batch")
    @RequirePermission("config.system")
    public Result updateBatch(@RequestBody Map<String, String> params) {
        sysConfigService.updateBatch(params);
        return Result.success();
    }

    @PutMapping("/admin/config")
    @RequirePermission("config.system")
    public Result update(@RequestBody Map<String, String> params) {
        String key = params.get("configKey");
        String value = params.get("configValue");
        if (key == null || key.isBlank()) {
            throw new CustomerException("配置键不能为空");
        }
        sysConfigService.updateConfig(key, value == null ? "" : value);
        return Result.success();
    }
}
