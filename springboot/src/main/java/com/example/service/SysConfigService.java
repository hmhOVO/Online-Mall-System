package com.example.service;

import com.example.entity.SysConfig;
import com.example.exception.CustomerException;
import com.example.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    public SysConfigService(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    public List<SysConfig> listAll() {
        return Arrays.asList(
                getOrCreate("deepseek.api_key", "DeepSeek API Key", ""),
                getOrCreate("deepseek.base_url", "DeepSeek Base URL", "https://api.deepseek.com"),
                getOrCreate("deepseek.model", "DeepSeek 文本模型", "deepseek-v4-flash"),
                getOrCreate("deepseek.timeout", "DeepSeek 超时时间(秒)", "60"),
                getOrCreate("ai.rate.task_split", "任务拆解限流(次/分钟)", "10"),
                getOrCreate("ai.rate.risk_analyze", "风险分析限流(次/分钟)", "10"),
                getOrCreate("overdue.warning.days", "逾期预警阈值（天）", "3"),
                getOrCreate("sse.enabled", "SSE流式开关（1开启/0关闭）", "1")
        );
    }

    public void updateBatch(Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            updateConfig(key, value);
        }
    }

    public void updateConfig(String key, String value) {
        SysConfig config = sysConfigMapper.selectByKey(key);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
        }
        config.setConfigValue(value);
        if (config.getId() == null) {
            sysConfigMapper.insert(config);
        } else {
            sysConfigMapper.updateById(config);
        }
    }

    public SysConfig getByKey(String key) {
        return sysConfigMapper.selectByKey(key);
    }

    private SysConfig getOrCreate(String key, String desc, String defaultValue) {
        SysConfig config = sysConfigMapper.selectByKey(key);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(defaultValue);
            config.setDescription(desc);
        }
        return config;
    }
}
