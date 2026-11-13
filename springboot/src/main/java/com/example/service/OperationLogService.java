package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.OperationLog;
import com.example.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void save(OperationLog log) {
        operationLogMapper.insert(log);
    }

    public Page<OperationLog> query(Integer pageNum, Integer pageSize, Integer userId,
                                    String operationType, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(userId != null, OperationLog::getUserId, userId)
                .eq(operationType != null && !operationType.isBlank(), OperationLog::getOperationType, operationType)
                .eq(OperationLog::getDeleted, 0)
                .orderByDesc(OperationLog::getCreatedAt);
        if (startDate != null) {
            wrapper.ge(OperationLog::getCreatedAt, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(OperationLog::getCreatedAt, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        return operationLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public Map<String, Object> getStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        List<Map<String, Object>> trendRows = operationLogMapper.selectOperationTrend(start, today.plusDays(1));
        Map<LocalDate, Long> trendMap = new HashMap<>();
        for (Map<String, Object> row : trendRows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            Long cnt = ((Number) row.get("cnt")).longValue();
            trendMap.put(date, cnt);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i <= ChronoUnit.DAYS.between(start, today); i++) {
            LocalDate date = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("cnt", trendMap.getOrDefault(date, 0L));
            trend.add(item);
        }

        List<Map<String, Object>> typeDistribution = operationLogMapper.selectOperationTypeDistribution();

        long total = 0;
        for (Map<String, Object> item : typeDistribution) {
            total += ((Number) item.get("cnt")).longValue();
        }
        long todayCount = trendMap.getOrDefault(today, 0L);
        long failedCount = operationLogMapper.countFailedLogs();

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", total);
        result.put("todayCount", todayCount);
        result.put("failedCount", failedCount);
        result.put("operationTrend", trend);
        result.put("operationTypeDistribution", typeDistribution);
        return result;
    }
}
