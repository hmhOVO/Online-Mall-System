package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.AiCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {

    @Select("select " +
            "ifnull(count(*), 0) as totalCount, " +
            "ifnull(sum(case when status = 'SUCCESS' then 1 else 0 end), 0) as successCount, " +
            "ifnull(sum(case when status = 'FAILED' then 1 else 0 end), 0) as failCount, " +
            "ifnull(sum(total_tokens), 0) as totalTokens, " +
            "ifnull(avg(latency_ms), 0) as avgLatency " +
            "from ai_call_log " +
            "where created_at >= #{startTime}")
    Map<String, Object> selectStatsSince(@Param("startTime") LocalDateTime startTime);
}
