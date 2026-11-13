package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("select * from operation_log where deleted = 0 and project_id = #{projectId} order by created_at desc")
    List<OperationLog> selectByProjectId(@Param("projectId") Integer projectId);

    @Select("select date(created_at) as date, count(*) as cnt from operation_log " +
            "where deleted = 0 and created_at >= #{start} and created_at < #{end} " +
            "group by date(created_at) order by date")
    List<Map<String, Object>> selectOperationTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("select operation_type as operationType, count(*) as cnt from operation_log " +
            "where deleted = 0 group by operation_type order by cnt desc")
    List<Map<String, Object>> selectOperationTypeDistribution();

    @Select("select count(*) from operation_log where deleted = 0 and status = 0")
    long countFailedLogs();
}
