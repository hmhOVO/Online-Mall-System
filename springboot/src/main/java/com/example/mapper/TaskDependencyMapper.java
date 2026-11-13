package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.TaskDependency;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface TaskDependencyMapper extends BaseMapper<TaskDependency> {

    @Select("select depends_on_task_id from task_dependency where task_id = #{taskId} and deleted = 0")
    List<Integer> selectDependsOnIds(@Param("taskId") Integer taskId);

    @Select("select * from task_dependency where task_id = #{taskId} and deleted = 0")
    List<TaskDependency> selectByTaskId(@Param("taskId") Integer taskId);

    @Select("<script>select * from task_dependency where (task_id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "or depends_on_task_id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>) " +
            "and deleted = 0</script>")
    List<TaskDependency> selectByTaskIdSet(@Param("ids") Collection<Integer> ids);

    @Select("<script>select task_id, depends_on_task_id from task_dependency " +
            "where task_id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "and deleted = 0</script>")
    List<TaskDependency> selectDependsOnByTaskIds(@Param("ids") Collection<Integer> ids);

    @Delete("DELETE FROM task_dependency WHERE task_id = #{taskId}")
    void physicalDeleteByTaskId(@Param("taskId") Integer taskId);

    @Delete("DELETE FROM task_dependency WHERE task_id = #{id} OR depends_on_task_id = #{id}")
    void physicalDeleteByTaskIdOrDependsOn(@Param("id") Integer id);
}
