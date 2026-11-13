package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.TaskComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {

    @Select("select tc.*, u.username, u.nickname from task_comment tc left join sys_user u on tc.user_id = u.id where tc.task_id = #{taskId} and tc.deleted = 0 order by tc.created_at asc")
    List<TaskComment> selectByTaskId(@Param("taskId") Integer taskId);

    @Select("<script>select tc.*, u.username, u.nickname from task_comment tc left join sys_user u on tc.user_id = u.id " +
            "where tc.task_id in " +
            "<foreach collection='taskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "and tc.deleted = 0 order by tc.created_at asc</script>")
    List<TaskComment> selectByTaskIds(@Param("taskIds") Collection<Integer> taskIds);
}
