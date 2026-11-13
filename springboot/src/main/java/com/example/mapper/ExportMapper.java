package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExportMapper {

    @Select({"<script>",
            "select p.id, p.name, p.description, p.status, p.start_date as startDate, p.end_date as endDate, ",
            "p.created_at as createdAt, ",
            "cu.nickname as creatorName, ou.nickname as ownerName ",
            "from project_info p ",
            "left join sys_user cu on p.creator_id = cu.id ",
            "left join sys_user ou on p.owner_id = ou.id ",
            "where p.deleted = 0 ",
            "<if test='status != null and status != \"\"'>and p.status = #{status} </if>",
            "<if test='keyword != null and keyword != \"\"'>and (p.name like CONCAT('%', #{keyword}, '%') or p.description like CONCAT('%', #{keyword}, '%')) </if>",
            "<if test='scopeUserId != null'>and (p.owner_id = #{scopeUserId} or p.teacher_id = #{scopeUserId} or exists (select 1 from project_member pm where pm.project_id = p.id and pm.user_id = #{scopeUserId} and pm.deleted = 0)) </if>",
            "order by p.created_at desc",
            "</script>"})
    List<Map<String, Object>> selectProjectsForExport(@Param("status") String status,
                                                      @Param("keyword") String keyword,
                                                      @Param("scopeUserId") Integer scopeUserId);

    @Select({"<script>",
            "select t.id, t.title, t.description, t.priority, t.status, t.progress, ",
            "t.planned_hours as plannedHours, t.actual_hours as actualHours, ",
            "t.start_date as startDate, t.end_date as endDate, t.completed_at as completedAt, ",
            "p.name as projectName, au.nickname as assigneeName, cu.nickname as creatorName ",
            "from task_info t ",
            "join project_info p on t.project_id = p.id ",
            "left join sys_user au on t.assignee_id = au.id ",
            "left join sys_user cu on t.creator_id = cu.id ",
            "where t.deleted = 0 and p.deleted = 0 ",
            "<if test='projectId != null'>and t.project_id = #{projectId} </if>",
            "<if test='status != null and status != \"\"'>and t.status = #{status} </if>",
            "<if test='priority != null and priority != \"\"'>and t.priority = #{priority} </if>",
            "<if test='assigneeId != null'>and t.assignee_id = #{assigneeId} </if>",
            "<if test='scopeUserId != null'>and (p.owner_id = #{scopeUserId} or p.teacher_id = #{scopeUserId} or exists (select 1 from project_member pm where pm.project_id = p.id and pm.user_id = #{scopeUserId} and pm.deleted = 0)) </if>",
            "order by t.created_at desc",
            "</script>"})
    List<Map<String, Object>> selectTasksForExport(@Param("projectId") Integer projectId,
                                                   @Param("status") String status,
                                                   @Param("priority") String priority,
                                                   @Param("assigneeId") Integer assigneeId,
                                                   @Param("scopeUserId") Integer scopeUserId);

    @Select({"<script>",
            "select t.id, t.title, t.description, t.priority, t.status, t.progress, ",
            "t.planned_hours as plannedHours, t.actual_hours as actualHours, ",
            "t.start_date as startDate, t.end_date as endDate, t.completed_at as completedAt, ",
            "p.name as projectName, au.nickname as assigneeName, cu.nickname as creatorName ",
            "from task_info t ",
            "join project_info p on t.project_id = p.id ",
            "left join sys_user au on t.assignee_id = au.id ",
            "left join sys_user cu on t.creator_id = cu.id ",
            "where t.deleted = 0 and p.deleted = 0 ",
            "and t.assignee_id = #{userId} ",
            "and ( ",
            "  (t.created_at &gt;= #{weekStart} and t.created_at &lt;= #{weekEnd}) ",
            "  or (t.completed_at &gt;= #{weekStart} and t.completed_at &lt;= #{weekEnd}) ",
            "  or (t.status in ('IN_PROGRESS', 'TODO', 'REVIEW', 'BLOCKED') and t.end_date &gt;= #{weekStart}) ",
            ") ",
            "order by t.created_at desc",
            "</script>"})
    List<Map<String, Object>> selectPersonalWeekTasks(@Param("userId") Integer userId,
                                                      @Param("weekStart") java.time.LocalDateTime weekStart,
                                                      @Param("weekEnd") java.time.LocalDateTime weekEnd);

    @Select({"<script>",
            "select p.name as projectName, u.username as username, u.nickname as nickname, ",
            "count(t.id) as totalTasks, ",
            "sum(case when t.status = 'DONE' then 1 else 0 end) as doneTasks, ",
            "sum(case when t.score is not null then 1 else 0 end) as scoredTaskCount, ",
            "round(avg(case when t.score is not null then t.score end), 1) as averageScore ",
            "from task_info t ",
            "join project_info p on t.project_id = p.id ",
            "left join sys_user u on t.assignee_id = u.id ",
            "where t.deleted = 0 and p.deleted = 0 and t.assignee_id is not null ",
            "<if test='projectId != null'>and t.project_id = #{projectId} </if>",
            "<if test='teacherId != null'>and (p.owner_id = #{teacherId} or p.teacher_id = #{teacherId}) </if>",
            "<if test='keyword != null and keyword != \"\"'>and (p.name like CONCAT('%', #{keyword}, '%') or u.nickname like CONCAT('%', #{keyword}, '%') or u.username like CONCAT('%', #{keyword}, '%')) </if>",
            "group by t.project_id, p.name, t.assignee_id, u.username, u.nickname ",
            "order by p.name, averageScore desc ",
            "</script>"})
    List<Map<String, Object>> selectStudentScoresForExport(@Param("projectId") Integer projectId,
                                                           @Param("keyword") String keyword,
                                                           @Param("teacherId") Integer teacherId);
}
