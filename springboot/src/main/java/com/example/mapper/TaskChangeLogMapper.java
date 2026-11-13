package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.TaskChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskChangeLogMapper extends BaseMapper<TaskChangeLog> {

    @Select("select tcl.*, u.nickname as changed_by_name from task_change_log tcl " +
            "left join sys_user u on tcl.changed_by = u.id " +
            "where tcl.task_id = #{taskId} order by tcl.changed_at desc")
    List<TaskChangeLog> selectByTaskId(@Param("taskId") Integer taskId);
}
