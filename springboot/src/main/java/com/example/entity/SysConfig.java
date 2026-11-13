package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private String configKey;
    private String configValue;
    private String description;

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
