package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.CurrentUserContext;
import com.example.common.ProjectConstants;
import com.example.common.TaskConstants;
import com.example.entity.Project;
import com.example.entity.ProjectMember;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.DashboardMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ProjectMemberMapper;
import com.example.mapper.TaskMapper;
import com.example.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;

    public DashboardService(DashboardMapper dashboardMapper, ProjectMapper projectMapper,
                            ProjectMemberMapper projectMemberMapper, UserMapper userMapper,
                            TaskMapper taskMapper) {
        this.dashboardMapper = dashboardMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
        this.taskMapper = taskMapper;
    }

    public Map<String, Object> personal() {
        User current = CurrentUserContext.get();
        Integer userId = current.getId();
        LocalDate today = LocalDate.now();

        int projects = dashboardMapper.countParticipatingProjects(userId);
        int completed = dashboardMapper.countCompletedTasks(userId);
        int total = dashboardMapper.countTotalTasksByAssignee(userId);
        int overdue = dashboardMapper.countOverdueTasks(userId, today);
        int todo = dashboardMapper.countTodoTasks(userId);
        double overdueRate = total == 0 ? 0 : BigDecimal.valueOf(overdue * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        Map<String, Object> hours = dashboardMapper.sumHoursByAssignee(userId);
        double planned = toDouble(hours.get("planned"));
        double actual = toDouble(hours.get("actual"));
        double hoursRatio = planned == 0 ? 0 : BigDecimal.valueOf(actual * 100.0 / planned)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        List<Map<String, Object>> trend = build7DaysTrend(userId, null, today);
        List<Map<String, Object>> statusDist = buildPersonalStatusDistribution(userId);

        List<Map<String, Object>> todayTasks = dashboardMapper.selectTodayTasks(userId, today);
        for (Map<String, Object> task : todayTasks) {
            Object deadline = task.get("deadline");
            if (deadline != null) {
                LocalDate d = ((java.sql.Date) deadline).toLocalDate();
                task.put("isOverdue", d.isBefore(today));
            }
            task.put("statusLabel", getStatusLabel((String) task.get("status")));
        }

        List<Map<String, Object>> recentTasks = dashboardMapper.selectRecentTasks(userId);
        for (Map<String, Object> task : recentTasks) {
            task.put("statusLabel", getStatusLabel((String) task.get("status")));
        }

        Map<String, Object> scoreAgg = dashboardMapper.sumScoreByAssignee(userId);
        Map<String, Object> scoreSummary = new HashMap<>();
        scoreSummary.put("scoredCount", toLong(scoreAgg.get("scoredCount")));
        scoreSummary.put("avgScore", scoreAgg.get("avgScore") == null ? 0 : toDouble(scoreAgg.get("avgScore")));
        scoreSummary.put("totalScore", scoreAgg.get("totalScore") == null ? 0 : toDouble(scoreAgg.get("totalScore")));
        scoreSummary.put("maxScore", scoreAgg.get("maxScore") == null ? 0 : toDouble(scoreAgg.get("maxScore")));
        List<Map<String, Object>> myScoredTasks = dashboardMapper.selectMyScoredTasks(userId);

        List<Map<String, Object>> participatingProjects = dashboardMapper.selectParticipatingProjects(userId);
        for (Map<String, Object> project : participatingProjects) {
            int totalTasks = ((Number) project.get("totalTasks")).intValue();
            int completedTasks = ((Number) project.get("completedTasks")).intValue();
            int progress = totalTasks == 0 ? 0 : (completedTasks * 100 / totalTasks);
            project.put("progress", progress);
            project.put("pendingTasks", totalTasks - completedTasks);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("projectCount", projects);
        result.put("completedTaskCount", completed);
        result.put("overdueRate", overdueRate);
        result.put("hoursRatio", hoursRatio);
        result.put("plannedHours", planned);
        result.put("actualHours", actual);
        result.put("trend", trend);
        result.put("statusDistribution", statusDist);
        result.put("todoCount", todo);
        result.put("overdueCount", overdue);
        result.put("todayTasks", todayTasks);
        result.put("recentTasks", recentTasks);
        result.put("projects", participatingProjects);
        result.put("scoreSummary", scoreSummary);
        result.put("myScoredTasks", myScoredTasks);
        return result;
    }

    private String getStatusLabel(String status) {
        Map<String, String> map = new HashMap<>();
        map.put("TODO", "待处理");
        map.put("IN_PROGRESS", "进行中");
        map.put("REVIEW", "审核中");
        map.put("DONE", "已完成");
        return map.getOrDefault(status, status);
    }

    public Map<String, Object> project(Integer projectId) {
        User current = CurrentUserContext.get();
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomerException("项目不存在");
        }
        checkProjectAccess(project, current);
        boolean canViewPrivacy = canViewProjectPrivacyData(project, current);

        LocalDate today = LocalDate.now();
        int total = dashboardMapper.countProjectTasks(projectId);
        List<Map<String, Object>> statusDist = dashboardMapper.selectProjectStatusDistribution(projectId);
        List<Map<String, Object>> completedTrend = build7DaysTrend(null, projectId, today);
        List<Map<String, Object>> burndown = buildBurndown(projectId, total, today);

        Map<String, Object> result = new HashMap<>();
        result.put("totalTasks", total);
        result.put("statusDistribution", statusDist);
        result.put("progressTrend", completedTrend);
        result.put("burndown", burndown);

        if (canViewPrivacy) {
            List<Map<String, Object>> workload = dashboardMapper.selectProjectMemberWorkload(projectId);
            fillUserNames(workload);
            List<Map<String, Object>> hours = dashboardMapper.selectProjectHours(projectId);
            fillUserNames(hours);
            result.put("memberWorkload", workload);
            result.put("hoursStats", hours);
        } else {
            result.put("memberWorkload", Collections.emptyList());
            result.put("hoursStats", Collections.emptyList());
        }
        return result;
    }

    public Map<String, Object> teacher() {
        User current = CurrentUserContext.get();
        Integer userId = current.getId();
        LocalDate today = LocalDate.now();

        int projectCount = dashboardMapper.countManagedProjects(userId);
        int totalTasks = dashboardMapper.countManagedProjectTasks(userId);
        int pendingReview = dashboardMapper.countManagedPendingReviewTasks(userId);
        int overdue = dashboardMapper.countManagedOverdueTasks(userId, today);
        int memberCount = dashboardMapper.countManagedMembers(userId);
        List<Map<String, Object>> projects = dashboardMapper.selectManagedProjects(userId, today);

        List<Map<String, Object>> reviewTasks = dashboardMapper.selectManagedReviewTasks(userId);
        List<Map<String, Object>> overdueTasksList = dashboardMapper.selectManagedOverdueTasks(userId, today);
        List<Map<String, Object>> workload = dashboardMapper.selectManagedWorkload(userId);
        List<Map<String, Object>> workloadByProject = dashboardMapper.selectManagedWorkloadByProject(userId);

        List<Map<String, Object>> trend = build7DaysTrendForTeacher(userId, today);
        List<Map<String, Object>> trendByProject = build7DaysTrendByProjectForTeacher(userId, today);
        List<Map<String, Object>> statusDistribution = dashboardMapper.selectManagedProjectStatusDistribution(userId);
        statusDistribution = fillProjectStatusDistribution(statusDistribution);

        double completionRate = totalTasks == 0 ? 0 : BigDecimal.valueOf(
                dashboardMapper.countManagedProjectTasksWithStatus(userId, "DONE") * 100.0 / totalTasks)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        Map<String, Object> result = new HashMap<>();
        result.put("projectCount", projectCount);
        result.put("totalTasks", totalTasks);
        result.put("pendingReviewTasks", pendingReview);
        result.put("overdueTasks", overdue);
        result.put("memberCount", memberCount);
        result.put("completionRate", completionRate);
        result.put("projects", projects);
        result.put("reviewTasks", reviewTasks);
        result.put("overdueTasksList", overdueTasksList);
        result.put("workload", workload);
        result.put("workloadByProject", workloadByProject);
        result.put("trend", trend);
        result.put("trendByProject", trendByProject);
        result.put("statusDistribution", statusDistribution);
        return result;
    }

    public List<Map<String, Object>> memberLoad() {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("用户未登录");
        }
        List<Map<String, Object>> list = dashboardMapper.selectMemberLoad(current.getId());
        for (Map<String, Object> item : list) {
            int total = ((Number) item.getOrDefault("totalTasks", 0)).intValue();
            int done = ((Number) item.getOrDefault("doneTasks", 0)).intValue();
            double completionRate = total == 0 ? 0 : BigDecimal.valueOf(done * 100.0 / total)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            item.put("completionRate", completionRate);
        }
        return list;
    }

    /**
     * 管理员全局业务总览
     */
    public Map<String, Object> adminBusinessOverview() {
        User current = CurrentUserContext.get();
        if (current == null || !RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("无权限访问");
        }
        LocalDate today = LocalDate.now();

        // 项目总览
        int projectCount = dashboardMapper.countTotalProjects();
        int activeProjectCount = dashboardMapper.countActiveProjects();
        List<Map<String, Object>> projects = dashboardMapper.selectAllProjectsForAdmin();

        // 任务总览
        int taskCount = dashboardMapper.countTotalTasks();
        int completedTaskCount = dashboardMapper.countSystemCompletedTasks();
        int overdueTaskCount = dashboardMapper.countSystemOverdueTasks();
        List<Map<String, Object>> taskStatusDist = dashboardMapper.selectSystemTaskStatusDistribution();

        // 成员负载（全局）
        List<Map<String, Object>> memberWorkload = dashboardMapper.selectGlobalMemberWorkload();
        for (Map<String, Object> item : memberWorkload) {
            int total = ((Number) item.getOrDefault("totalTasks", 0)).intValue();
            int done = ((Number) item.getOrDefault("doneTasks", 0)).intValue();
            double completionRate = total == 0 ? 0 : BigDecimal.valueOf(done * 100.0 / total)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            item.put("completionRate", completionRate);
        }

        // 即将过期任务
        List<Map<String, Object>> upcomingTasks = dashboardMapper.selectSystemUpcomingTasks(today);

        double taskCompletionRate = taskCount == 0 ? 0 : BigDecimal.valueOf(completedTaskCount * 100.0 / taskCount)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        Map<String, Object> result = new HashMap<>();
        // 项目数据
        result.put("projectCount", projectCount);
        result.put("activeProjectCount", activeProjectCount);
        result.put("projects", projects);
        // 任务数据
        result.put("taskCount", taskCount);
        result.put("completedTaskCount", completedTaskCount);
        result.put("overdueTaskCount", overdueTaskCount);
        result.put("taskCompletionRate", taskCompletionRate);
        result.put("taskStatusDistribution", taskStatusDist);
        result.put("upcomingTasks", upcomingTasks);
        // 成员数据
        result.put("memberWorkload", memberWorkload);
        return result;
    }

    private List<Map<String, Object>> build7DaysTrendForTeacher(Integer userId, LocalDate end) {
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> rows = dashboardMapper.selectManagedCompletedTrend(userId, start, end.plusDays(1));
        Map<LocalDate, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            Long cnt = ((Number) row.get("cnt")).longValue();
            map.put(date, cnt);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", map.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> build7DaysTrendByProjectForTeacher(Integer userId, LocalDate end) {
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> rows = dashboardMapper.selectManagedCompletedTrendByProject(userId, start, end.plusDays(1));
        
        Map<Integer, Map<String, Object>> projectMap = new HashMap<>();
        Set<LocalDate> allDates = new TreeSet<>();
        
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            allDates.add(date);
            Integer projectId = ((Number) row.get("projectId")).intValue();
            String projectName = (String) row.get("projectName");
            Long cnt = ((Number) row.get("cnt")).longValue();
            
            Map<String, Object> projectData = projectMap.computeIfAbsent(projectId, k -> {
                Map<String, Object> map = new HashMap<>();
                map.put("projectId", k);
                map.put("projectName", projectName);
                map.put("data", new ArrayList<Map<String, Object>>());
                return map;
            });
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) projectData.get("data");
            Map<String, Object> dateData = new HashMap<>();
            dateData.put("date", date.toString());
            dateData.put("count", cnt);
            dataList.add(dateData);
        }
        
        for (Map<String, Object> projectData : projectMap.values()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) projectData.get("data");
            Map<String, Long> existingData = new HashMap<>();
            for (Map<String, Object> item : dataList) {
                existingData.put((String) item.get("date"), ((Number) item.get("count")).longValue());
            }
            
            List<Map<String, Object>> filledData = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate date = start.plusDays(i);
                String dateStr = date.toString();
                Map<String, Object> item = new HashMap<>();
                item.put("date", dateStr);
                item.put("count", existingData.getOrDefault(dateStr, 0L));
                filledData.add(item);
            }
            projectData.put("data", filledData);
        }
        
        return new ArrayList<>(projectMap.values());
    }

    public Map<String, Object> admin() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        // 用户与业务统计
        int userCount = dashboardMapper.countTotalUsers();
        int adminCount = dashboardMapper.countAdmins();
        int teacherCount = dashboardMapper.countTeachers();
        int memberCount = dashboardMapper.countMembers();
        int todayNewUsers = dashboardMapper.countTodayNewUsers();
        int activeUsers7Days = dashboardMapper.countActiveUsers7Days();
        List<Map<String, Object>> userStatusDistribution = dashboardMapper.selectUserStatusDistribution();
        int projectCount = dashboardMapper.countTotalProjects();
        int taskCount = dashboardMapper.countTotalTasks();
        int completedTaskCount = dashboardMapper.countSystemCompletedTasks();
        int overdueTaskCount = dashboardMapper.countSystemOverdueTasks();
        int activeProjectCount = dashboardMapper.countActiveProjects();

        double taskCompletionRate = taskCount == 0 ? 0 : BigDecimal.valueOf(completedTaskCount * 100.0 / taskCount)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        List<Map<String, Object>> projectTrend = buildAdminTrend(start, today, dashboardMapper.selectProjectCreationTrend(start, today.plusDays(1)));
        List<Map<String, Object>> taskTrend = buildAdminTrend(start, today, dashboardMapper.selectTaskCreationTrend(start, today.plusDays(1)));
        List<Map<String, Object>> completionTrend = buildAdminTrend(start, today, dashboardMapper.selectTaskCompletionTrend(start, today.plusDays(1)));
        List<Map<String, Object>> topMembers = dashboardMapper.selectTopMembers();

        // 系统维度统计
        int todayLoginCount = dashboardMapper.countTodayLoginUsers();
        int totalLoginCount = dashboardMapper.countTotalLoginLogs();
        int onlineUserCount = dashboardMapper.countOnlineUsers();
        int totalOperationLogs = dashboardMapper.countTotalOperationLogs();
        int todayOperationLogs = dashboardMapper.countTodayOperationLogs();
        int totalAiCalls = dashboardMapper.countTotalAiCalls();
        int todayAiCalls = dashboardMapper.countTodayAiCalls();
        List<Map<String, Object>> recentLoginRecords = dashboardMapper.selectRecentLoginRecords();

        // 登录趋势（近7天）
        List<Map<String, Object>> loginTrend = buildLoginTrend(start, today);

        Map<String, Object> result = new HashMap<>();
        // 业务数据
        result.put("userCount", userCount);
        result.put("adminCount", adminCount);
        result.put("teacherCount", teacherCount);
        result.put("memberCount", memberCount);
        result.put("todayNewUsers", todayNewUsers);
        result.put("activeUsers7Days", activeUsers7Days);
        result.put("userStatusDistribution", userStatusDistribution);
        result.put("projectCount", projectCount);
        result.put("taskCount", taskCount);
        result.put("completedTaskCount", completedTaskCount);
        result.put("overdueTaskCount", overdueTaskCount);
        result.put("activeProjectCount", activeProjectCount);
        result.put("taskCompletionRate", taskCompletionRate);
        result.put("projectTrend", projectTrend);
        result.put("taskTrend", taskTrend);
        result.put("completionTrend", completionTrend);
        result.put("topMembers", topMembers);
        // 系统数据
        result.put("todayLoginCount", todayLoginCount);
        result.put("totalLoginCount", totalLoginCount);
        result.put("onlineUserCount", onlineUserCount);
        result.put("totalOperationLogs", totalOperationLogs);
        result.put("todayOperationLogs", todayOperationLogs);
        result.put("totalAiCalls", totalAiCalls);
        result.put("todayAiCalls", todayAiCalls);
        result.put("recentLoginRecords", recentLoginRecords);
        result.put("loginTrend", loginTrend);
        return result;
    }

    private List<Map<String, Object>> buildLoginTrend(LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = dashboardMapper.selectLoginTrend(start, end.plusDays(1));
        Map<LocalDate, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            Long cnt = ((Number) row.get("cnt")).longValue();
            map.put(date, cnt);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i <= ChronoUnit.DAYS.between(start, end); i++) {
            LocalDate date = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", map.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildAdminTrend(LocalDate start, LocalDate end, List<Map<String, Object>> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            Long cnt = ((Number) row.get("cnt")).longValue();
            map.put(date, cnt);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i <= ChronoUnit.DAYS.between(start, end); i++) {
            LocalDate date = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", map.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    public Map<String, Object> aiStatistics() {
        User current = CurrentUserContext.get();
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomerException("无权限查看 AI 统计");
        }
        Map<String, Object> overview = dashboardMapper.selectAiOverview();
        long total = toLong(overview.get("total"));
        long success = toLong(overview.get("success"));
        long failed = toLong(overview.get("failed"));
        long tokens = toLong(overview.get("tokens"));

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> tokenTrend = buildAiTokenTrend(today);
        List<Map<String, Object>> sceneDist = dashboardMapper.selectAiSceneDistribution();
        List<Map<String, Object>> typeDist = dashboardMapper.selectAiTypeDistribution();

        Map<String, Object> result = new HashMap<>();
        result.put("totalCalls", total);
        result.put("successRate", total == 0 ? 0 : BigDecimal.valueOf(success * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
        result.put("failureRate", total == 0 ? 0 : BigDecimal.valueOf(failed * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
        result.put("totalTokens", tokens);
        result.put("tokenTrend", tokenTrend);
        result.put("sceneDistribution", sceneDist);
        result.put("aiTypeDistribution", typeDist);
        return result;
    }

    private List<Map<String, Object>> build7DaysTrend(Integer userId, Integer projectId, LocalDate end) {
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> rows = userId != null
                ? dashboardMapper.selectPersonalCompletedTrend(userId, start, end.plusDays(1))
                : dashboardMapper.selectProjectCompletedTrend(projectId, start, end.plusDays(1));
        Map<LocalDate, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            Long cnt = ((Number) row.get("cnt")).longValue();
            map.put(date, cnt);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", map.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildBurndown(Integer projectId, int total, LocalDate end) {
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> rows = dashboardMapper.selectProjectCompletedTrend(projectId, start, end.plusDays(1));
        Map<LocalDate, Long> completedMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            Long cnt = ((Number) row.get("cnt")).longValue();
            completedMap.put(date, cnt);
        }
        List<Map<String, Object>> burndown = new ArrayList<>();
        long cumulative = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            cumulative += completedMap.getOrDefault(date, 0L);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("remaining", Math.max(0, total - cumulative));
            burndown.add(item);
        }
        return burndown;
    }

    private List<Map<String, Object>> buildPersonalStatusDistribution(Integer userId) {
        LambdaQueryWrapper<com.example.entity.Task> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(com.example.entity.Task::getAssigneeId, userId).eq(com.example.entity.Task::getDeleted, 0);
        List<com.example.entity.Task> tasks = taskMapper.selectList(wrapper);
        Map<String, Long> count = new HashMap<>();
        for (com.example.entity.Task t : tasks) {
            count.merge(t.getStatus(), 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : count.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", e.getKey());
            item.put("count", e.getValue());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildAiTokenTrend(LocalDate end) {
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> rows = dashboardMapper.selectAiTokenTrend(start, end.plusDays(1));
        Map<LocalDate, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            Long tokens = ((Number) row.get("tokens")).longValue();
            map.put(date, tokens);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("tokens", map.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    private void checkProjectAccess(Project project, User current) {
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return;
        }
        if (Objects.equals(project.getTeacherId(), current.getId())) {
            return;
        }
        if (project.getOwnerId() != null && project.getOwnerId().equals(current.getId())) {
            return;
        }
        LambdaQueryWrapper<ProjectMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectMember::getProjectId, project.getId()).eq(ProjectMember::getUserId, current.getId());
        if (projectMemberMapper.selectCount(wrapper) == 0) {
            throw new CustomerException("无权限查看该项目");
        }
    }

    private boolean canViewProjectPrivacyData(Project project, User current) {
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return true;
        }
        if (Objects.equals(project.getTeacherId(), current.getId())) {
            return true;
        }
        if (project.getOwnerId() != null && project.getOwnerId().equals(current.getId())) {
            return true;
        }
        LambdaQueryWrapper<ProjectMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectMember::getProjectId, project.getId())
                .eq(ProjectMember::getUserId, current.getId());
        return projectMemberMapper.selectCount(wrapper) > 0;
    }

    private List<Map<String, Object>> fillProjectStatusDistribution(List<Map<String, Object>> data) {
        Set<String> existingStatus = new HashSet<>();
        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> item : data) {
            String status = (String) item.get("status");
            existingStatus.add(status);
            countMap.put(status, ((Number) item.get("cnt")).longValue());
        }

        String[] allStatus = {
                ProjectConstants.PROJECT_STATUS_NOT_STARTED,
                ProjectConstants.PROJECT_STATUS_IN_PROGRESS,
                ProjectConstants.PROJECT_STATUS_DELAYED,
                ProjectConstants.PROJECT_STATUS_COMPLETED
        };

        List<Map<String, Object>> result = new ArrayList<>();
        for (String status : allStatus) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", status);
            item.put("count", countMap.getOrDefault(status, 0L));
            result.add(item);
        }
        return result;
    }

    private void fillUserNames(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Object uid = row.get("userId");
            if (uid == null) {
                row.put("username", "未分配");
                continue;
            }
            User user = userMapper.selectById(((Number) uid).intValue());
            String name = user == null ? uid.toString()
                    : (isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername());
            row.put("username", name);
        }
    }

    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private long toLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
