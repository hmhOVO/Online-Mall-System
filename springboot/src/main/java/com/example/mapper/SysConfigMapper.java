package com.example.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    default SysConfig selectByKey(String key) {
        return selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .eq(SysConfig::getDeleted, 0)
                .last("limit 1"));
    }
}
