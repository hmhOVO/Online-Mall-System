package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    @Select("select pm.*, u.username, u.nickname, u.email, u.avatar, " +
            "(select count(*) from task_info where assignee_id = pm.user_id and project_id = pm.project_id and deleted = 0) as taskCount, " +
            "(select count(*) from task_info where assignee_id = pm.user_id and project_id = pm.project_id and status = 'DONE' and deleted = 0) as completedCount " +
            "from project_member pm left join sys_user u on pm.user_id = u.id where pm.project_id = #{projectId} and pm.deleted = 0")
    List<ProjectMember> selectMembersByProjectId(@Param("projectId") Integer projectId);

    @Select("select * from project_member where project_id = #{projectId} and deleted = 0")
    List<ProjectMember> selectByProjectId(@Param("projectId") Integer projectId);

    @Select("select * from project_member where project_id = #{projectId} and user_id = #{userId} and deleted = 1")
    ProjectMember selectDeletedMember(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    @Update("update project_member set deleted = 0, joined_at = #{joinedAt}, updated_at = #{updatedAt} " +
            "where id = #{id} and deleted = 1")
    int restoreMember(@Param("id") Integer id,
                      @Param("joinedAt") LocalDateTime joinedAt, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("update project_member set deleted = 0 where project_id = #{projectId} and deleted = 1")
    int restoreMembersByProjectId(Integer projectId);

    @Select("select pm.*, u.username, u.nickname, u.email from project_member pm " +
            "left join sys_user u on pm.user_id = u.id " +
            "where pm.user_id = #{userId} and pm.deleted = 0")
    List<ProjectMember> selectMembersByUserId(@Param("userId") Integer userId);

    @Select("select " +
            "  pm.user_id as userId, " +
            "  u.username, " +
            "  u.nickname, " +
            "  u.email, " +
            "  u.avatar, " +
            "  count(distinct pm.project_id) as projectCount, " +
            "  group_concat(distinct p.name separator ',') as projectNames, " +
            "  group_concat(distinct pm.project_id separator ',') as projectIdsStr, " +
            "  (select count(*) from task_info t where t.assignee_id = pm.user_id and t.deleted = 0 and exists (" +
            "    select 1 from project_info p2 where p2.id = t.project_id and p2.deleted = 0 " +
            "    and (exists (" +
            "      select 1 from project_member pm2 where pm2.project_id = p2.id and pm2.user_id = #{userId} and pm2.deleted = 0" +
            "    ) or p2.owner_id = #{userId} or p2.teacher_id = #{userId})" +
            "  )) as taskCount, " +
            "  (select count(*) from task_info t where t.assignee_id = pm.user_id and t.status = 'DONE' and t.deleted = 0 and exists (" +
            "    select 1 from project_info p2 where p2.id = t.project_id and p2.deleted = 0 " +
            "    and (exists (" +
            "      select 1 from project_member pm2 where pm2.project_id = p2.id and pm2.user_id = #{userId} and pm2.deleted = 0" +
            "    ) or p2.owner_id = #{userId} or p2.teacher_id = #{userId})" +
            "  )) as completedCount, " +
            "  min(pm.joined_at) as firstJoinedAt " +
            "from project_member pm " +
            "left join sys_user u on pm.user_id = u.id " +
            "left join project_info p on pm.project_id = p.id " +
            "where pm.deleted = 0 and p.deleted = 0 " +
            "and (exists (" +
            "  select 1 from project_member pm3 where pm3.project_id = p.id and pm3.user_id = #{userId} and pm3.deleted = 0" +
            ") or p.owner_id = #{userId} or p.teacher_id = #{userId}) " +
            "group by pm.user_id, u.username, u.nickname, u.email, u.avatar " +
            "order by firstJoinedAt asc")
    List<Map<String, Object>> selectMemberSummary(@Param("userId") Integer userId);

    @Select("select t.*, p.name as projectName " +
            "from task_info t " +
            "left join project_info p on t.project_id = p.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.assignee_id = #{memberUserId} " +
            "and (p.owner_id = #{userId} or p.teacher_id = #{userId}) " +
            "order by t.created_at desc")
    List<Map<String, Object>> selectMemberAllTasks(@Param("userId") Integer userId, @Param("memberUserId") Integer memberUserId);
}
