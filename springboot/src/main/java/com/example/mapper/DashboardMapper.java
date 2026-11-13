package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    @Select("<script>" +
            "select count(distinct p.id) from project_info p " +
            "where p.deleted = 0 " +
            "and (p.owner_id = #{userId} " +
            "  or exists (select 1 from project_member m where m.project_id = p.id and m.user_id = #{userId}))" +
            "</script>")
    int countParticipatingProjects(@Param("userId") Integer userId);

    @Select("select count(*) from task_info where deleted = 0 and assignee_id = #{userId} and status = 'DONE'")
    int countCompletedTasks(@Param("userId") Integer userId);

    @Select("select count(*) from task_info where deleted = 0 and assignee_id = #{userId}")
    int countTotalTasksByAssignee(@Param("userId") Integer userId);

    @Select("select count(*) from task_info where deleted = 0 and assignee_id = #{userId} " +
            "and status != 'DONE' and end_date < #{today}")
    int countOverdueTasks(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Select("select ifnull(sum(planned_hours),0) as planned, ifnull(sum(actual_hours),0) as actual " +
            "from task_info where deleted = 0 and assignee_id = #{userId}")
    Map<String, Object> sumHoursByAssignee(@Param("userId") Integer userId);

    @Select("select date(completed_at) as date, count(*) as cnt from task_info " +
            "where deleted = 0 and assignee_id = #{userId} and status = 'DONE' " +
            "and completed_at >= #{start} and completed_at < #{end} " +
            "group by date(completed_at) order by date")
    List<Map<String, Object>> selectPersonalCompletedTrend(@Param("userId") Integer userId,
                                                           @Param("start") LocalDate start,
                                                           @Param("end") LocalDate end);

    @Select("select t.id, t.title, t.status, t.end_date as deadline, t.reviewer_id as reviewerId, " +
            "  p.id as projectId, p.name as projectName " +
            "from task_info t left join project_info p on t.project_id = p.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.assignee_id = #{userId} " +
            "and t.status != 'DONE' and (t.end_date = #{today} or t.end_date < #{today}) " +
            "order by case when t.end_date < #{today} then 0 else 1 end, t.end_date asc limit 10")
    List<Map<String, Object>> selectTodayTasks(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Select("select t.id, t.title, t.status, t.end_date as deadline, " +
            "  p.id as projectId, p.name as projectName, t.created_at as createdAt " +
            "from task_info t left join project_info p on t.project_id = p.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.assignee_id = #{userId} " +
            "order by t.created_at desc limit 10")
    List<Map<String, Object>> selectRecentTasks(@Param("userId") Integer userId);

    @Select("select count(*) as scoredCount, round(avg(t.score),1) as avgScore, " +
            "  sum(t.score) as totalScore, max(t.score) as maxScore " +
            "from task_info t left join project_info p on t.project_id = p.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.assignee_id = #{userId} and t.score is not null")
    Map<String, Object> sumScoreByAssignee(@Param("userId") Integer userId);

    @Select("select t.id, t.title, t.score, t.status, p.id as projectId, p.name as projectName " +
            "from task_info t left join project_info p on t.project_id = p.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.assignee_id = #{userId} and t.score is not null " +
            "order by t.created_at desc limit 10")
    List<Map<String, Object>> selectMyScoredTasks(@Param("userId") Integer userId);

    @Select("select p.id, p.name, p.status, p.start_date as startDate, p.end_date as endDate, " +
            "  u.nickname as ownerName, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id) as totalTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id and t.status = 'DONE') as completedTasks, " +
            "  ifnull((select sum(t.planned_hours) from task_info t where t.deleted = 0 and t.project_id = p.id and t.assignee_id = #{userId}), 0) as plannedHours, " +
            "  ifnull((select sum(t.actual_hours) from task_info t where t.deleted = 0 and t.project_id = p.id and t.assignee_id = #{userId}), 0) as actualHours " +
            "from project_info p left join sys_user u on p.owner_id = u.id " +
            "where p.deleted = 0 and (p.owner_id = #{userId} " +
            "  or exists (select 1 from project_member m where m.project_id = p.id and m.user_id = #{userId})) " +
            "order by p.created_at desc")
    List<Map<String, Object>> selectParticipatingProjects(@Param("userId") Integer userId);

    @Select("select count(*) from task_info where deleted = 0 and assignee_id = #{userId} and status = 'TODO'")
    int countTodoTasks(@Param("userId") Integer userId);

    @Select("select status, count(*) as cnt from task_info where deleted = 0 and project_id = #{projectId} group by status")
    List<Map<String, Object>> selectProjectStatusDistribution(@Param("projectId") Integer projectId);

    @Select("select assignee_id as userId, count(*) as cnt from task_info where deleted = 0 and project_id = #{projectId} group by assignee_id")
    List<Map<String, Object>> selectProjectMemberWorkload(@Param("projectId") Integer projectId);

    @Select("select date(completed_at) as date, count(*) as cnt from task_info " +
            "where deleted = 0 and project_id = #{projectId} and status = 'DONE' " +
            "and completed_at >= #{start} and completed_at < #{end} " +
            "group by date(completed_at) order by date")
    List<Map<String, Object>> selectProjectCompletedTrend(@Param("projectId") Integer projectId,
                                                          @Param("start") LocalDate start,
                                                          @Param("end") LocalDate end);

    @Select("select count(*) from task_info where deleted = 0 and project_id = #{projectId}")
    int countProjectTasks(@Param("projectId") Integer projectId);

    @Select("select assignee_id as userId, ifnull(sum(planned_hours),0) as planned, ifnull(sum(actual_hours),0) as actual " +
            "from task_info where deleted = 0 and project_id = #{projectId} group by assignee_id")
    List<Map<String, Object>> selectProjectHours(@Param("projectId") Integer projectId);

    @Select("select count(*) as total, " +
            "sum(case when status = 'SUCCESS' then 1 else 0 end) as success, " +
            "sum(case when status = 'FAILED' then 1 else 0 end) as failed, " +
            "ifnull(sum(total_tokens),0) as tokens " +
            "from ai_call_log where deleted = 0")
    Map<String, Object> selectAiOverview();

    @Select("select date(created_at) as date, count(*) as cnt, ifnull(sum(total_tokens),0) as tokens " +
            "from ai_call_log where deleted = 0 and created_at >= #{start} and created_at < #{end} " +
            "group by date(created_at) order by date")
    List<Map<String, Object>> selectAiTokenTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("select scene, count(*) as cnt " +
            "from ai_call_log " +
            "where deleted = 0 group by scene")
    List<Map<String, Object>> selectAiSceneDistribution();

    @Select("select ai_type as aiType, count(*) as cnt, " +
            "  sum(case when status = 'SUCCESS' then 1 else 0 end) as success, " +
            "  sum(case when status = 'FAILED' then 1 else 0 end) as failed, " +
            "  ifnull(sum(total_tokens),0) as tokens " +
            "from ai_call_log where deleted = 0 group by ai_type")
    List<Map<String, Object>> selectAiTypeDistribution();

    @Select("select count(distinct p.id) from project_info p " +
            "where p.deleted = 0 and (p.owner_id = #{userId} " +
            "  or p.teacher_id = #{userId})")
    int countManagedProjects(@Param("userId") Integer userId);

    @Select("select count(*) from task_info t " +
            "where t.deleted = 0 and exists (" +
            "  select 1 from project_info p where p.id = t.project_id and p.deleted = 0 " +
            "  and (p.owner_id = #{userId} or p.teacher_id = #{userId})" +
            ")")
    int countManagedProjectTasks(@Param("userId") Integer userId);

    @Select("select count(*) from task_info t " +
            "where t.deleted = 0 and t.status = 'REVIEW' and t.reviewer_id = #{userId}")
    int countManagedPendingReviewTasks(@Param("userId") Integer userId);

    @Select("select count(*) from task_info t " +
            "where t.deleted = 0 and t.status != 'DONE' and t.end_date < #{today} and exists (" +
            "  select 1 from project_info p where p.id = t.project_id and p.deleted = 0 " +
            "  and (p.owner_id = #{userId} or p.teacher_id = #{userId})" +
            ")")
    int countManagedOverdueTasks(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Select("select count(distinct m.user_id) from project_member m " +
            "where exists (" +
            "  select 1 from project_info p where p.id = m.project_id and p.deleted = 0 " +
            "  and (p.owner_id = #{userId} or p.teacher_id = #{userId})" +
            ")")
    int countManagedMembers(@Param("userId") Integer userId);

    @Select("select p.id, p.name, p.status, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id) as totalTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id and t.status = 'DONE') as completedTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id and t.status = 'REVIEW') as pendingReviewTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id and t.status != 'DONE' and t.end_date < #{today}) as overdueTasks " +
            "from project_info p " +
            "where p.deleted = 0 and (p.owner_id = #{userId} " +
            "  or p.teacher_id = #{userId}) " +
            "order by p.created_at desc")
    List<Map<String, Object>> selectManagedProjects(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Select("select " +
            "  (select count(distinct p.id) from project_info p where p.deleted = 0 and (p.owner_id = #{userId} or exists (select 1 from project_member m where m.project_id = p.id and m.user_id = #{userId}))) as projectCount, " +
            "  (select count(*) from task_info where deleted = 0 and assignee_id = #{userId} and status = 'DONE') as completedTaskCount, " +
            "  (select count(*) from task_info where deleted = 0 and assignee_id = #{userId} and status = 'TODO') as todoCount, " +
            "  (select count(*) from task_info where deleted = 0 and assignee_id = #{userId} and status != 'DONE' and end_date < CURDATE()) as overdueCount, " +
            "  (select ifnull(sum(planned_hours),0) from task_info where deleted = 0 and assignee_id = #{userId}) as plannedHours, " +
            "  (select ifnull(sum(actual_hours),0) from task_info where deleted = 0 and assignee_id = #{userId}) as actualHours, " +
            "  (select round(ifnull(sum(case when status = 'DONE' then 1 else 0 end) * 100.0 / count(*), 0), 2) from task_info where deleted = 0 and assignee_id = #{userId}) as completionRate")
    Map<String, Object> getPersonalStats(@Param("userId") Integer userId);

    @Select("select u.nickname, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = #{projectId} and t.assignee_id = pm.user_id) as totalTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = #{projectId} and t.assignee_id = pm.user_id and t.status = 'DONE') as completedTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = #{projectId} and t.assignee_id = pm.user_id and t.status = 'TODO') as todoTasks, " +
            "  (select ifnull(sum(t.actual_hours),0) from task_info t where t.deleted = 0 and t.project_id = #{projectId} and t.assignee_id = pm.user_id) as actualHours " +
            "from project_member pm left join sys_user u on pm.user_id = u.id " +
            "where pm.project_id = #{projectId} and pm.deleted = 0")
    List<Map<String, Object>> selectMemberStatsByProject(@Param("projectId") Integer projectId);

    @Select("select d.date, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.assignee_id = #{userId} and t.status = 'DONE' and date(t.completed_at) = d.date) as completedCount, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.assignee_id = #{userId} and date(t.created_at) = d.date) as createdCount " +
            "from (" +
            "  select DATE_ADD(#{start}, INTERVAL 0 DAY) as date union all " +
            "  select DATE_ADD(#{start}, INTERVAL 1 DAY) union all " +
            "  select DATE_ADD(#{start}, INTERVAL 2 DAY) union all " +
            "  select DATE_ADD(#{start}, INTERVAL 3 DAY) union all " +
            "  select DATE_ADD(#{start}, INTERVAL 4 DAY) union all " +
            "  select DATE_ADD(#{start}, INTERVAL 5 DAY) union all " +
            "  select DATE_ADD(#{start}, INTERVAL 6 DAY)" +
            ") d " +
            "order by d.date")
    List<Map<String, Object>> selectTaskTrendByDate(@Param("userId") Integer userId,
                                                     @Param("start") LocalDate start,
                                                     @Param("end") LocalDate end);

    @Select("select t.id, t.title, t.priority, t.end_date as deadline, " +
            "  p.id as projectId, p.name as projectName, " +
            "  u.nickname as assigneeName " +
            "from task_info t " +
            "left join project_info p on t.project_id = p.id " +
            "left join sys_user u on t.assignee_id = u.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.status = 'REVIEW' " +
            "and t.reviewer_id = #{userId} " +
            "order by t.created_at desc limit 10")
    List<Map<String, Object>> selectManagedReviewTasks(@Param("userId") Integer userId);

    @Select("select t.id, t.title, t.end_date as deadline, " +
            "  p.id as projectId, p.name as projectName, " +
            "  u.nickname as assigneeName " +
            "from task_info t " +
            "left join project_info p on t.project_id = p.id " +
            "left join sys_user u on t.assignee_id = u.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.status != 'DONE' and t.end_date < #{today} " +
            "and (p.owner_id = #{userId} or p.teacher_id = #{userId}) " +
            "order by t.end_date asc limit 10")
    List<Map<String, Object>> selectManagedOverdueTasks(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Select("select pm.user_id as userId, u.nickname as username, " +
            "  count(t.id) as totalTasks, " +
            "  sum(case when t.status = 'DONE' then 1 else 0 end) as completedTasks " +
            "from project_member pm " +
            "left join sys_user u on pm.user_id = u.id " +
            "left join task_info t on t.assignee_id = pm.user_id and t.deleted = 0 " +
            "where pm.deleted = 0 and exists (" +
            "  select 1 from project_info p where p.id = pm.project_id and p.deleted = 0 " +
            "  and (p.owner_id = #{userId} or p.teacher_id = #{userId})" +
            ") " +
            "group by pm.user_id, u.nickname " +
            "order by completedTasks desc")
    List<Map<String, Object>> selectManagedWorkload(@Param("userId") Integer userId);

    @Select("select p.id as projectId, p.name as projectName, " +
            "  pm.user_id as userId, u.nickname as username, " +
            "  count(t.id) as totalTasks, " +
            "  sum(case when t.status = 'DONE' then 1 else 0 end) as completedTasks " +
            "from project_info p " +
            "left join project_member pm on pm.project_id = p.id and pm.deleted = 0 " +
            "left join sys_user u on pm.user_id = u.id " +
            "left join task_info t on t.assignee_id = pm.user_id and t.project_id = p.id and t.deleted = 0 " +
            "where p.deleted = 0 and (p.owner_id = #{userId} or p.teacher_id = #{userId}) " +
            "group by p.id, p.name, pm.user_id, u.nickname " +
            "order by p.id asc, completedTasks desc")
    List<Map<String, Object>> selectManagedWorkloadByProject(@Param("userId") Integer userId);

    @Select("select date(t.completed_at) as date, count(*) as cnt " +
            "from task_info t " +
            "where t.deleted = 0 and t.status = 'DONE' and exists (" +
            "  select 1 from project_info p where p.id = t.project_id and p.deleted = 0 " +
            "  and (p.owner_id = #{userId} or p.teacher_id = #{userId})" +
            ") " +
            "and t.completed_at >= #{start} and t.completed_at < #{end} " +
            "group by date(t.completed_at) order by date")
    List<Map<String, Object>> selectManagedCompletedTrend(@Param("userId") Integer userId,
                                                          @Param("start") LocalDate start,
                                                          @Param("end") LocalDate end);

    @Select("select date(t.completed_at) as date, p.id as projectId, p.name as projectName, count(*) as cnt " +
            "from task_info t " +
            "left join project_info p on t.project_id = p.id " +
            "where t.deleted = 0 and t.status = 'DONE' and p.deleted = 0 and exists (" +
            "  select 1 from project_info p2 where p2.id = t.project_id and p2.deleted = 0 " +
            "  and (p2.owner_id = #{userId} or p2.teacher_id = #{userId})" +
            ") " +
            "and t.completed_at >= #{start} and t.completed_at < #{end} " +
            "group by date(t.completed_at), p.id, p.name order by date, p.id")
    List<Map<String, Object>> selectManagedCompletedTrendByProject(@Param("userId") Integer userId,
                                                                   @Param("start") LocalDate start,
                                                                   @Param("end") LocalDate end);

    @Select("select p.status, count(*) as cnt " +
            "from project_info p " +
            "where p.deleted = 0 and (p.owner_id = #{userId} " +
            "  or p.teacher_id = #{userId}) " +
            "group by p.status")
    List<Map<String, Object>> selectManagedProjectStatusDistribution(@Param("userId") Integer userId);

    @Select("select count(*) from task_info t " +
            "where t.deleted = 0 and t.status = #{status} and exists (" +
            "  select 1 from project_info p where p.id = t.project_id and p.deleted = 0 " +
            "  and (p.owner_id = #{userId} or p.teacher_id = #{userId})" +
            ")")
    int countManagedProjectTasksWithStatus(@Param("userId") Integer userId, @Param("status") String status);

    @Select("select count(*) as userCount from sys_user where deleted = 0")
    int countTotalUsers();

    @Select("select count(*) as adminCount from sys_user where deleted = 0 and role = 'ADMIN'")
    int countAdmins();

    @Select("select count(*) as teacherCount from sys_user where deleted = 0 and role = 'TEACHER'")
    int countTeachers();

    @Select("select count(*) as memberCount from sys_user where deleted = 0 and role = 'STUDENT'")
    int countMembers();

    @Select("select count(*) from sys_user where deleted = 0 and date(created_at) = curdate()")
    int countTodayNewUsers();

    @Select("select count(distinct user_id) from sys_login_log " +
            "where deleted = 0 and login_time >= date_sub(now(), interval 7 day) and status = 1")
    int countActiveUsers7Days();

    @Select("select status, count(*) as cnt from sys_user where deleted = 0 group by status")
    List<Map<String, Object>> selectUserStatusDistribution();

    @Select("select count(*) as projectCount from project_info where deleted = 0")
    int countTotalProjects();

    @Select("select count(*) as taskCount from task_info where deleted = 0")
    int countTotalTasks();

    @Select("select count(*) as completedTaskCount from task_info where deleted = 0 and status = 'DONE'")
    int countSystemCompletedTasks();

    @Select("select count(*) as overdueTaskCount from task_info where deleted = 0 and status != 'DONE' and end_date < CURDATE()")
    int countSystemOverdueTasks();

    @Select("select count(distinct project_id) as activeProjectCount from task_info where deleted = 0 and status = 'IN_PROGRESS'")
    int countActiveProjects();

    @Select("select date(p.created_at) as date, count(*) as cnt from project_info p " +
            "where p.deleted = 0 and p.created_at >= #{start} and p.created_at < #{end} " +
            "group by date(p.created_at) order by date")
    List<Map<String, Object>> selectProjectCreationTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("select date(t.created_at) as date, count(*) as cnt from task_info t " +
            "where t.deleted = 0 and t.created_at >= #{start} and t.created_at < #{end} " +
            "group by date(t.created_at) order by date")
    List<Map<String, Object>> selectTaskCreationTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("select date(t.completed_at) as date, count(*) as cnt from task_info t " +
            "where t.deleted = 0 and t.status = 'DONE' and t.completed_at >= #{start} and t.completed_at < #{end} " +
            "group by date(t.completed_at) order by date")
    List<Map<String, Object>> selectTaskCompletionTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("select u.nickname, u.username, count(t.id) as taskCount, " +
            "  sum(case when t.status = 'DONE' then 1 else 0 end) as completedCount, " +
            "  sum(case when t.status != 'DONE' and t.end_date < CURDATE() then 1 else 0 end) as overdueCount " +
            "from sys_user u left join task_info t on t.assignee_id = u.id and t.deleted = 0 " +
            "where u.deleted = 0 and u.role = 'STUDENT' " +
            "group by u.id, u.nickname, u.username " +
            "order by completedCount desc limit 10")
    List<Map<String, Object>> selectTopMembers();

    @Select("select u.id as userId, u.nickname, u.username, " +
            "  count(t.id) as totalTasks, " +
            "  sum(case when t.status = 'TODO' then 1 else 0 end) as todoTasks, " +
            "  sum(case when t.status = 'IN_PROGRESS' then 1 else 0 end) as inProgressTasks, " +
            "  sum(case when t.status = 'REVIEW' then 1 else 0 end) as reviewTasks, " +
            "  sum(case when t.status = 'DONE' then 1 else 0 end) as doneTasks, " +
            "  sum(case when t.status != 'DONE' and t.end_date < CURDATE() then 1 else 0 end) as overdueTasks, " +
            "  ifnull(sum(t.planned_hours), 0) as plannedHours, " +
            "  ifnull(sum(t.actual_hours), 0) as actualHours " +
            "from project_member pm " +
            "join project_info p on pm.project_id = p.id and p.deleted = 0 " +
            "join sys_user u on pm.user_id = u.id and u.deleted = 0 and u.status = 'ENABLED' " +
            "left join task_info t on t.assignee_id = u.id and t.project_id = p.id and t.deleted = 0 " +
            "where pm.deleted = 0 " +
            "and exists (" +
            "  select 1 from project_info p2 " +
            "  where p2.id = pm.project_id and p2.deleted = 0 " +
            "  and (p2.owner_id = #{userId} or p2.teacher_id = #{userId})" +
            ") " +
            "group by u.id, u.nickname, u.username " +
            "order by totalTasks desc")
    List<Map<String, Object>> selectMemberLoad(@Param("userId") Integer userId);

    @Select("select count(distinct user_id) from sys_login_log " +
            "where date(login_time) = curdate() and status = 1 and deleted = 0")
    int countTodayLoginUsers();

    @Select("select count(*) from sys_login_log where deleted = 0")
    int countTotalLoginLogs();

    @Select("select count(distinct user_id) from sys_login_log " +
            "where login_time >= date_sub(now(), interval 30 minute) and status = 1 and deleted = 0")
    int countOnlineUsers();

    @Select("select count(*) from operation_log where deleted = 0")
    int countTotalOperationLogs();

    @Select("select count(*) from operation_log where date(created_at) = curdate()")
    int countTodayOperationLogs();

    @Select("select count(*) from ai_call_log where deleted = 0")
    int countTotalAiCalls();

    @Select("select count(*) from ai_call_log where date(created_at) = curdate()")
    int countTodayAiCalls();

    @Select("select l.id, l.user_id as userId, u.nickname, u.username, " +
            "  l.ip as ipAddress, l.user_agent as userAgent, l.login_time as loginTime, " +
            "  case l.status when 1 then 'SUCCESS' else 'FAILED' end as status " +
            "from sys_login_log l left join sys_user u on l.user_id = u.id " +
            "where l.deleted = 0 " +
            "order by l.login_time desc limit 10")
    List<Map<String, Object>> selectRecentLoginRecords();

    @Select("select date(login_time) as date, count(*) as cnt from sys_login_log " +
            "where deleted = 0 and login_time >= #{start} and login_time < #{end} " +
            "group by date(login_time) order by date")
    List<Map<String, Object>> selectLoginTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // ========== 管理员全局业务总览 ==========

    @Select("select p.id, p.name, p.status, p.start_date as startDate, p.end_date as endDate, " +
            "  u.nickname as ownerName, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id) as totalTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id and t.status = 'DONE') as completedTasks " +
            "from project_info p left join sys_user u on p.owner_id = u.id " +
            "where p.deleted = 0 order by p.created_at desc")
    List<Map<String, Object>> selectAllProjectsForAdmin();

    @Select("select status, count(*) as cnt from task_info where deleted = 0 group by status")
    List<Map<String, Object>> selectSystemTaskStatusDistribution();

    @Select("select u.id as userId, u.nickname as username, " +
            "  count(t.id) as totalTasks, " +
            "  sum(case when t.status = 'TODO' then 1 else 0 end) as todoTasks, " +
            "  sum(case when t.status = 'IN_PROGRESS' then 1 else 0 end) as inProgressTasks, " +
            "  sum(case when t.status = 'REVIEW' then 1 else 0 end) as reviewTasks, " +
            "  sum(case when t.status = 'DONE' then 1 else 0 end) as doneTasks, " +
            "  sum(case when t.status != 'DONE' and t.end_date < CURDATE() then 1 else 0 end) as overdueTasks " +
            "from sys_user u " +
            "left join task_info t on t.assignee_id = u.id and t.deleted = 0 " +
            "where u.deleted = 0 and u.role != 'ADMIN' " +
            "group by u.id, u.nickname " +
            "order by totalTasks desc")
    List<Map<String, Object>> selectGlobalMemberWorkload();

    @Select("select t.id, t.title, t.priority, t.end_date as deadline, " +
            "  p.id as projectId, p.name as projectName, " +
            "  u.nickname as assigneeName " +
            "from task_info t " +
            "left join project_info p on t.project_id = p.id " +
            "left join sys_user u on t.assignee_id = u.id " +
            "where t.deleted = 0 and p.deleted = 0 and t.status != 'DONE' " +
            "and t.end_date >= #{today} and t.end_date <= DATE_ADD(#{today}, INTERVAL 3 DAY) " +
            "order by t.end_date asc limit 10")
    List<Map<String, Object>> selectSystemUpcomingTasks(@Param("today") LocalDate today);
}
