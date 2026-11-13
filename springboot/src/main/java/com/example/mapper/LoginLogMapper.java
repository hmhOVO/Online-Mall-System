package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    @Select("select date(login_time) as date, count(*) as cnt from sys_login_log " +
            "where deleted = 0 and login_time >= #{start} and login_time < #{end} " +
            "group by date(login_time) order by date")
    List<Map<String, Object>> selectCountByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("select " +
            "ifnull(count(*), 0) as totalCount, " +
            "ifnull(sum(case when status = 1 then 1 else 0 end), 0) as successCount, " +
            "ifnull(sum(case when status = 0 then 1 else 0 end), 0) as failCount, " +
            "ifnull(count(distinct username), 0) as userCount " +
            "from sys_login_log " +
            "where deleted = 0 and login_time >= #{startTime}")
    Map<String, Object> selectStatsSince(@Param("startTime") LocalDateTime startTime);
}
