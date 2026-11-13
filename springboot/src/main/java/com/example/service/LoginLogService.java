package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.LoginLog;
import com.example.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Service
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public LoginLogService(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    public void save(LoginLog log) {
        if (log.getLoginTime() == null) {
            log.setLoginTime(LocalDateTime.now());
        }
        loginLogMapper.insert(log);
    }

    public Page<LoginLog> query(Integer pageNum, Integer pageSize, String username,
                                Integer status, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<LoginLog> wrapper = Wrappers.lambdaQuery();
        wrapper.like(username != null && !username.isBlank(), LoginLog::getUsername, username)
                .eq(status != null, LoginLog::getStatus, status)
                .eq(LoginLog::getDeleted, 0)
                .orderByDesc(LoginLog::getLoginTime);
        if (startDate != null) {
            wrapper.ge(LoginLog::getLoginTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(LoginLog::getLoginTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        return loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public Map<String, Object> getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> todayStats = loginLogMapper.selectStatsSince(todayStart);
        Map<String, Object> totalStats = loginLogMapper.selectStatsSince(LocalDateTime.of(2000, 1, 1, 0, 0));

        todayStats.put("successRate", 0.0);
        long total = ((Number) totalStats.get("totalCount")).longValue();
        long success = ((Number) totalStats.get("successCount")).longValue();
        if (total > 0) {
            double rate = (double) success / total * 100;
            todayStats.put("successRate", Math.round(rate * 10) / 10.0);
        }

        todayStats.put("totalCount", totalStats.get("totalCount"));
        todayStats.put("totalUserCount", totalStats.get("userCount"));
        return todayStats;
    }
}
