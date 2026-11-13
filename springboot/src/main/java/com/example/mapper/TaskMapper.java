package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("select * from task_info where project_id = #{projectId} and deleted = 0")
    List<Task> selectByProjectId(@Param("projectId") Integer projectId);

    @Select("select * from task_info where parent_task_id = #{parentId} and deleted = 0")
    List<Task> selectChildren(@Param("parentId") Integer parentId);

    @Select({"<script>",
            "select t.*, au.nickname as assignee_name, cu.nickname as creator_name, ru.nickname as reviewer_name ",
            "from task_info t ",
            "left join sys_user au on t.assignee_id = au.id ",
            "left join sys_user cu on t.creator_id = cu.id ",
            "left join sys_user ru on t.reviewer_id = ru.id ",
            "where t.project_id = #{projectId} and t.deleted = 0 ",
            "<if test='statuses != null and statuses.size() > 0'>",
            "and t.status in ",
            "<foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>",
            "</if>",
            "<if test='assigneeId != null'>",
            "and t.assignee_id = #{assigneeId}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "and (t.title like concat('%', #{keyword}, '%') or t.description like concat('%', #{keyword}, '%'))",
            "</if>",
            "order by t.task_level asc, t.created_at desc",
            "</script>"})
    List<Task> selectByProjectIdWithUser(@Param("projectId") Integer projectId,
                                         @Param("statuses") Collection<String> statuses,
                                         @Param("assigneeId") Integer assigneeId,
                                         @Param("keyword") String keyword);

    @Select({"<script>",
            "select distinct t.*, p.name as project_name, au.nickname as assignee_name, cu.nickname as creator_name, ru.nickname as reviewer_name ",
            "from task_info t ",
            "join project_info p on t.project_id = p.id ",
            "left join project_member pm on pm.project_id = t.project_id and pm.user_id = #{userId} and pm.deleted = 0 ",
            "left join sys_user au on t.assignee_id = au.id ",
            "left join sys_user cu on t.creator_id = cu.id ",
            "left join sys_user ru on t.reviewer_id = ru.id ",
            "where t.deleted = 0 and p.deleted = 0 ",
            "and (pm.id is not null or p.owner_id = #{userId} or p.teacher_id = #{userId}) ",
            "<choose>",
            "<when test='relationType == \"responsible\"'>",
            "and t.assignee_id = #{userId}",
            "</when>",
            "<when test='relationType == \"created\"'>",
            "and t.creator_id = #{userId}",
            "</when>",
            "<when test='relationType == \"review\"'>",
            "and t.reviewer_id = #{userId} and t.status != 'DONE'",
            "</when>",
            "<otherwise>",
            "and (t.assignee_id = #{userId} or t.creator_id = #{userId} or t.reviewer_id = #{userId})",
            "</otherwise>",
            "</choose>",
            "<if test='statuses != null and statuses.size() > 0'>",
            "and t.status in ",
            "<foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "and (t.title like concat('%', #{keyword}, '%') or t.description like concat('%', #{keyword}, '%'))",
            "</if>",
            "order by t.end_date asc, t.created_at desc",
            "</script>"})
    List<Task> selectMyTasks(@Param("userId") Integer userId,
                             @Param("statuses") Collection<String> statuses,
                             @Param("keyword") String keyword,
                             @Param("relationType") String relationType);

    @Select({"<script>",
            "select distinct t.*, p.name as project_name, au.nickname as assignee_name, cu.nickname as creator_name, ru.nickname as reviewer_name ",
            "from task_info t ",
            "join project_info p on t.project_id = p.id ",
            "left join sys_user au on t.assignee_id = au.id ",
            "left join sys_user cu on t.creator_id = cu.id ",
            "left join sys_user ru on t.reviewer_id = ru.id ",
            "where t.deleted = 0 and p.deleted = 0 ",
            "and t.project_id in ",
            "<foreach collection='projectIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach>",
            "<if test='statuses != null and statuses.size() > 0'>",
            "and t.status in ",
            "<foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>",
            "</if>",
            "<if test='assigneeId != null'>",
            "and t.assignee_id = #{assigneeId}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "and (t.title like concat('%', #{keyword}, '%') or t.description like concat('%', #{keyword}, '%'))",
            "</if>",
            "order by t.end_date asc, t.created_at desc",
            "</script>"})
    List<Task> selectTrainingTasks(@Param("projectIds") Collection<Integer> projectIds,
                               @Param("statuses") Collection<String> statuses,
                               @Param("assigneeId") Integer assigneeId,
                               @Param("keyword") String keyword);

    @Select("select t.assignee_id as studentId, u.nickname as nickname, u.username as username, " +
            "count(*) as scoredTaskCount, round(avg(t.score), 1) as averageScore " +
            "from task_info t " +
            "left join sys_user u on t.assignee_id = u.id " +
            "where t.project_id = #{projectId} and t.deleted = 0 and t.status = 'DONE' and t.score is not null " +
            "group by t.assignee_id, u.nickname, u.username " +
            "order by averageScore desc")
    List<Map<String, Object>> selectScoreSummary(@Param("projectId") Integer projectId);

    @Select("select sg.id as groupId, sg.name as groupName, sg.group_no as groupNo, " +
            "(select count(*) from group_member gm where gm.group_id = sg.id and gm.deleted = 0) as memberCount, " +
            "(select count(*) from task_info t where t.project_id = sg.project_id and t.deleted = 0 and (t.group_id = sg.id " +
            "  or t.assignee_id in (select gm2.student_id from group_member gm2 where gm2.group_id = sg.id and gm2.deleted = 0))) as taskCount, " +
            "(select count(*) from task_info t where t.project_id = sg.project_id and t.deleted = 0 and t.status = 'DONE' and (t.group_id = sg.id " +
            "  or t.assignee_id in (select gm3.student_id from group_member gm3 where gm3.group_id = sg.id and gm3.deleted = 0))) as doneCount, " +
            "(select round(avg(t.score), 1) from task_info t where t.project_id = sg.project_id and t.deleted = 0 and t.status = 'DONE' " +
            "  and t.score is not null and (t.group_id = sg.id " +
            "  or t.assignee_id in (select gm4.student_id from group_member gm4 where gm4.group_id = sg.id and gm4.deleted = 0))) as averageScore " +
            "from student_group sg " +
            "where sg.project_id = #{projectId} and sg.deleted = 0 " +
            "order by sg.id")
    List<Map<String, Object>> selectGroupScoreSummary(@Param("projectId") Integer projectId);
}
