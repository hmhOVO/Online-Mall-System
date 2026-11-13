package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.CurrentUserContext;
import com.example.common.ProjectConstants;
import com.example.common.ProjectRoleLevel;
import com.example.common.TaskConstants;
import com.example.entity.*;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final TaskChangeLogMapper taskChangeLogMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final NotificationWebSocketService notificationWebSocketService;
    private final ProjectService projectService;
    private final TaskWorkflowService taskWorkflowService;
    private final TaskCommentService taskCommentService;
    private final TaskDependencyService taskDependencyService;
    private final TaskAttachmentService taskAttachmentService;
    private final GroupMemberMapper groupMemberMapper;
    private final StudentGroupMapper studentGroupMapper;

    public TaskService(TaskMapper taskMapper,
                       TaskCommentMapper taskCommentMapper,
                       TaskChangeLogMapper taskChangeLogMapper,
                       ProjectMapper projectMapper,
                       ProjectMemberMapper projectMemberMapper,
                       UserMapper userMapper,
                       NotificationService notificationService,
                       NotificationWebSocketService notificationWebSocketService,
                       ProjectService projectService,
                       TaskWorkflowService taskWorkflowService,
                       TaskCommentService taskCommentService,
                       TaskDependencyService taskDependencyService,
                       TaskAttachmentService taskAttachmentService,
                       GroupMemberMapper groupMemberMapper,
                       StudentGroupMapper studentGroupMapper) {
        this.taskMapper = taskMapper;
        this.taskCommentMapper = taskCommentMapper;
        this.taskChangeLogMapper = taskChangeLogMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.notificationWebSocketService = notificationWebSocketService;
        this.projectService = projectService;
        this.taskWorkflowService = taskWorkflowService;
        this.taskCommentService = taskCommentService;
        this.taskDependencyService = taskDependencyService;
        this.taskAttachmentService = taskAttachmentService;
        this.groupMemberMapper = groupMemberMapper;
        this.studentGroupMapper = studentGroupMapper;
    }

    @Transactional
    public void createTask(Task task) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        validateTaskFields(task);
        checkCreatePermission(task, current);
        if (isProjectTeacher(task.getProjectId(), current)) {
            // 教师可下发到指定小组：任务进入该组任务池，由组长二次分配给组员
            if (task.getGroupId() != null) {
                validateTaskGroup(task.getProjectId(), task.getGroupId());
                task.setAssigneeId(null);
            }
        } else {
            // 评分只由指导教师填写
            task.setScore(null);
            // 组长创建未分配任务时，放入本组任务池，由组员认领
            if (task.getAssigneeId() == null) {
                task.setGroupId(groupMemberMapper.selectLedGroupId(task.getProjectId(), current.getId()));
            }
        }

        Integer level = 1;
        if (task.getParentTaskId() != null) {
            Task parent = getTask(task.getParentTaskId());
            if (!parent.getProjectId().equals(task.getProjectId())) {
                throw new CustomerException("父任务不属于当前项目");
            }
            level = parent.getTaskLevel() + 1;
            if (level > TaskConstants.MAX_LEVEL) {
                throw new CustomerException("任务层级不能超过3级");
            }
        }
        task.setTaskLevel(level);
        task.setStatus(TaskConstants.STATUS_TODO);
        task.setProgress(0);
        task.setCreatorId(current.getId());
        taskMapper.insert(task);

        taskDependencyService.saveDependencies(task.getId(), task.getProjectId(), task.getDependencyIds());

        Project project = projectMapper.selectById(task.getProjectId());
        if (project != null && ProjectConstants.PROJECT_STATUS_NOT_STARTED.equals(project.getStatus())) {
            LambdaUpdateWrapper<Project> wrapper = Wrappers.lambdaUpdate();
            wrapper.eq(Project::getId, task.getProjectId())
                    .set(Project::getStatus, ProjectConstants.PROJECT_STATUS_IN_PROGRESS);
            projectMapper.update(null, wrapper);
        }
    }

    @Transactional
    public void claimTask(Integer id) {
        taskWorkflowService.claimTask(id);
    }

    @Transactional
    public void updateTask(Integer id, Task task) {
        User current = CurrentUserContext.get();
        Task existing = getTask(id);
        validateProjectAccess(existing.getProjectId(), current, false);
        checkProjectWritable(existing.getProjectId());
        checkTaskManagePermission(existing, current, task.getAssigneeId());

        boolean isClosed = TaskConstants.STATUS_DONE.equals(existing.getStatus())
                || TaskConstants.STATUS_REVIEW.equals(existing.getStatus());
        if (isClosed) {
            throw new CustomerException("已收尾任务不可编辑");
        }
        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(existing.getAssigneeId()) && isClosed) {
            throw new CustomerException("收尾状态禁止变更组长");
        }
        if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty() && isClosed) {
            throw new CustomerException("收尾状态禁止变更前置依赖");
        }

        if (task.getReviewerId() != null && task.getAssigneeId() != null
                && task.getReviewerId().equals(task.getAssigneeId())) {
            throw new CustomerException("审核人不能是任务组长自己");
        }

        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id)
                .set(task.getTitle() != null, Task::getTitle, task.getTitle())
                .set(task.getDescription() != null, Task::getDescription, task.getDescription())
                .set(task.getPriority() != null, Task::getPriority, task.getPriority())
                .set(task.getPlannedHours() != null, Task::getPlannedHours, task.getPlannedHours())
                .set(task.getStartDate() != null, Task::getStartDate, task.getStartDate())
                .set(task.getEndDate() != null, Task::getEndDate, task.getEndDate())
                .set(true, Task::getAssigneeId, task.getAssigneeId())
                .set(task.getReviewerId() != null, Task::getReviewerId, task.getReviewerId())
                .set(task.getTaskType() != null, Task::getTaskType, task.getTaskType())
                .set(task.getDifficulty() != null, Task::getDifficulty, task.getDifficulty())
                .set(task.getScore() != null && isProjectTeacher(existing.getProjectId(), current),
                        Task::getScore, task.getScore());
        taskMapper.update(null, wrapper);

        if (task.getDependencyIds() != null) {
            taskDependencyService.saveDependencies(id, existing.getProjectId(), task.getDependencyIds());
        }

        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(existing.getAssigneeId())) {
            notifyAssigneeChanged(existing, task.getAssigneeId());
        }
    }

    @Transactional
    public void deleteTask(Integer id) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        checkTaskManagePermission(task, current, null);
        deleteTaskCascade(id);
    }

    private void deleteTaskCascade(Integer id) {
        List<Task> children = taskMapper.selectChildren(id);
        for (Task child : children) {
            deleteTaskCascade(child.getId());
        }
        taskDependencyService.deleteByTaskIdOrDependsOn(id);
        taskAttachmentService.deleteByTaskId(id);
        taskMapper.deleteById(id);
    }

    public Task getTaskDetail(Integer id) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        fillTaskInfoBatch(Collections.singletonList(task), true);
        if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty()) {
            List<Task> deps = taskMapper.selectList(
                    Wrappers.lambdaQuery(Task.class)
                            .in(Task::getId, task.getDependencyIds())
                            .eq(Task::getDeleted, 0));
            fillTaskInfoBatch(deps, false);
            task.setDependencies(deps);
        }
        return task;
    }

    public List<Task> listProjectTasks(Integer projectId, List<String> statuses, List<String> priorities,
                                       Integer assigneeId, Boolean onlyOverdue, String keyword) {
        User current = CurrentUserContext.get();
        validateProjectAccess(projectId, current, false);

        List<String> queryStatuses = expandBlockedStatus(statuses);
        List<Task> tasks = taskMapper.selectByProjectIdWithUser(projectId, queryStatuses, assigneeId, keyword);
        if (priorities != null && !priorities.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> priorities.contains(t.getPriority()))
                    .collect(Collectors.toList());
        }

        fillTaskInfoBatch(tasks, false);

        LocalDate today = LocalDate.now();
        for (Task task : tasks) {
            boolean overdue = task.getEndDate() != null && task.getEndDate().isBefore(today)
                    && !TaskConstants.STATUS_DONE.equals(task.getStatus());
            task.setOverdue(overdue);
        }
        if (Boolean.TRUE.equals(onlyOverdue)) {
            tasks = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getOverdue())).collect(Collectors.toList());
        }
        return buildTree(tasks);
    }

    public List<Task> listMyTasks(List<String> statuses, String keyword, String relationType) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("用户未登录");
        }
        List<Task> tasks = taskMapper.selectMyTasks(current.getId(), statuses, keyword, relationType);
        LocalDate today = LocalDate.now();
        for (Task task : tasks) {
            boolean overdue = task.getEndDate() != null && task.getEndDate().isBefore(today)
                    && !TaskConstants.STATUS_DONE.equals(task.getStatus());
            task.setOverdue(overdue);
        }
        return tasks;
    }

    public List<Task> listTrainingTasks(List<Integer> projectIds, List<String> statuses, List<String> priorities,
                                    Integer assigneeId, String keyword) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("用户未登录");
        }

        // 计算当前用户可访问的项目集合（负责人/指导教师的项目 + 作为成员参与的项目）
        Set<Integer> accessibleProjectIds = new HashSet<>();
        LambdaQueryWrapper<Project> ownerWrapper = Wrappers.lambdaQuery();
        ownerWrapper.eq(Project::getOwnerId, current.getId()).eq(Project::getDeleted, 0);
        projectMapper.selectList(ownerWrapper).forEach(p -> accessibleProjectIds.add(p.getId()));

        LambdaQueryWrapper<Project> teacherWrapper = Wrappers.lambdaQuery();
        teacherWrapper.eq(Project::getTeacherId, current.getId()).eq(Project::getDeleted, 0);
        projectMapper.selectList(teacherWrapper).forEach(p -> accessibleProjectIds.add(p.getId()));

        LambdaQueryWrapper<ProjectMember> pmWrapper = Wrappers.lambdaQuery();
        pmWrapper.eq(ProjectMember::getUserId, current.getId()).eq(ProjectMember::getDeleted, 0);
        projectMemberMapper.selectList(pmWrapper).forEach(pm -> accessibleProjectIds.add(pm.getProjectId()));

        if (accessibleProjectIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 如果传入 projectIds，则与可访问集合取交集，防止越权访问
        if (projectIds != null && !projectIds.isEmpty()) {
            projectIds = projectIds.stream()
                    .filter(accessibleProjectIds::contains)
                    .distinct()
                    .collect(Collectors.toList());
            if (projectIds.isEmpty()) {
                return Collections.emptyList();
            }
        } else {
            projectIds = new ArrayList<>(accessibleProjectIds);
        }

        List<String> queryStatuses = expandBlockedStatus(statuses);
        List<Task> tasks = taskMapper.selectTrainingTasks(projectIds, queryStatuses, assigneeId, keyword);
        if (priorities != null && !priorities.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> priorities.contains(t.getPriority()))
                    .collect(Collectors.toList());
        }
        fillTaskInfoBatch(tasks, false);

        LocalDate today = LocalDate.now();
        for (Task task : tasks) {
            boolean overdue = task.getEndDate() != null && task.getEndDate().isBefore(today)
                    && !TaskConstants.STATUS_DONE.equals(task.getStatus());
            task.setOverdue(overdue);
        }
        return tasks;
    }

    public void updateStatus(Integer id, String newStatus) {
        taskWorkflowService.updateStatus(id, newStatus);
    }

    public void updateActualHours(Integer id, Double actualHours) {
        taskWorkflowService.updateActualHours(id, actualHours);
    }

    public void updateProgress(Integer id, Integer progress) {
        taskWorkflowService.updateProgress(id, progress);
    }

    public void submitReview(Integer id, Integer reviewerId) {
        taskWorkflowService.submitReview(id, reviewerId);
    }

    public void approveReview(Integer id, Double score) {
        taskWorkflowService.approveReview(id, score);
    }

    public void scoreTask(Integer id, Double score) {
        taskWorkflowService.scoreTask(id, score);
    }

    public void rejectReview(Integer id, String comment) {
        taskWorkflowService.rejectReview(id, comment);
    }

    public void withdrawReview(Integer id) {
        taskWorkflowService.withdrawReview(id);
    }

    public void completeDirectly(Integer id) {
        taskWorkflowService.completeDirectly(id);
    }

    public boolean checkDependencyCycle(Integer taskId, List<Integer> dependencyIds) {
        return taskDependencyService.checkDependencyCycle(taskId, dependencyIds);
    }

    public void addComment(Integer taskId, Integer parentCommentId, String content) {
        taskCommentService.addComment(taskId, parentCommentId, content);
    }

    public void resolveComment(Integer commentId, Boolean resolved) {
        taskCommentService.resolveComment(commentId, resolved);
    }

    public void updateComment(Integer commentId, String content) {
        taskCommentService.updateComment(commentId, content);
    }

    public void deleteComment(Integer commentId) {
        taskCommentService.deleteComment(commentId);
    }

    public List<Task> listOverdueTasks() {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Task> wrapper = Wrappers.lambdaQuery();
        wrapper.lt(Task::getEndDate, today)
                .ne(Task::getStatus, TaskConstants.STATUS_DONE)
                .eq(Task::getAssigneeId, current.getId())
                .orderByAsc(Task::getEndDate);
        List<Task> tasks = taskMapper.selectList(wrapper);
        fillTaskInfoBatch(tasks, false);
        return tasks;
    }

    public List<TaskChangeLog> getTaskChangeLogs(Integer taskId) {
        Task task = getTask(taskId);
        User current = CurrentUserContext.get();
        validateProjectAccess(task.getProjectId(), current, false);
        return taskChangeLogMapper.selectByTaskId(taskId);
    }

    private Task getTask(Integer id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new CustomerException("任务不存在");
        }
        return task;
    }

    private void checkCreatePermission(Task task, User current) {
        if (isProjectTeacher(task.getProjectId(), current)) {
            return;
        }
        if (groupMemberMapper.selectLedGroupId(task.getProjectId(), current.getId()) == null) {
            throw new CustomerException("仅小组组长或指导教师可创建任务");
        }
        if (task.getAssigneeId() == null) {
            // 组长可以创建待认领任务，但只进入本组任务池
            return;
        }
        if (!isGroupLeaderOf(task.getProjectId(), current.getId(), task.getAssigneeId())) {
            throw new CustomerException("仅可为自己小组的成员创建任务");
        }
    }

    private void checkTaskManagePermission(Task existing, User current, Integer newAssigneeId) {
        boolean isTeacher = isProjectTeacher(existing.getProjectId(), current);
        Integer ledGroupId = groupMemberMapper.selectLedGroupId(existing.getProjectId(), current.getId());
        boolean isLedGroupTask = existing.getGroupId() != null && existing.getGroupId().equals(ledGroupId);
        boolean allowed = isTeacher
                || (existing.getCreatorId() != null && existing.getCreatorId().equals(current.getId()))
                || isGroupLeaderOf(existing.getProjectId(), current.getId(), existing.getAssigneeId())
                || isLedGroupTask;
        if (!allowed) {
            throw new CustomerException("仅任务创建人、小组组长或指导教师可操作该任务");
        }
        if (newAssigneeId != null && !isTeacher && !isGroupLeaderOf(existing.getProjectId(), current.getId(), newAssigneeId)) {
            boolean self = newAssigneeId.equals(current.getId());
            boolean sameGroup = isLedGroupTask
                    && existing.getGroupId().equals(groupMemberMapper.selectGroupIdByStudent(existing.getProjectId(), newAssigneeId));
            if (!self && !sameGroup) {
                throw new CustomerException("只能将任务分配给本小组成员");
            }
        }
    }

    private void validateTaskGroup(Integer projectId, Integer groupId) {
        StudentGroup group = studentGroupMapper.selectById(groupId);
        if (group == null || !projectId.equals(group.getProjectId())) {
            throw new CustomerException("分组不存在或不属于当前项目");
        }
        if (groupMemberMapper.selectCount(Wrappers.lambdaQuery(GroupMember.class)
                .eq(GroupMember::getGroupId, groupId)) == 0) {
            throw new CustomerException("该分组暂无成员，无法下发任务");
        }
    }

    private boolean isProjectTeacher(Integer projectId, User user) {
        Project project = projectMapper.selectById(projectId);
        return project != null && Objects.equals(project.getTeacherId(), user.getId());
    }

    private boolean isGroupLeaderOf(Integer projectId, Integer leaderId, Integer studentId) {
        if (leaderId == null || studentId == null || leaderId.equals(studentId)) {
            return false;
        }
        Integer leader = groupMemberMapper.selectLeaderIdByStudent(projectId, studentId);
        return leader != null && leader.equals(leaderId);
    }

    private void validateProjectAccess(Integer projectId, User user, boolean requireManage) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomerException("项目不存在");
        }
        boolean isAdmin = RoleEnum.ADMIN.name().equals(user.getRole());
        boolean isOwner = project.getOwnerId() != null && project.getOwnerId().equals(user.getId());
        boolean isTeacher = Objects.equals(project.getTeacherId(), user.getId());
        LambdaQueryWrapper<ProjectMember> memberWrapper = Wrappers.lambdaQuery();
        memberWrapper.eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, user.getId());
        boolean isMember = projectMemberMapper.selectCount(memberWrapper) > 0;
        if (!isAdmin && !isMember && !isOwner && !isTeacher) {
            throw new CustomerException("无权限访问该项目");
        }
        if (requireManage) {
            if (isAdmin) {
                throw new CustomerException("系统管理员无项目管理权限，请使用指导教师账号操作");
            }
            ProjectRoleLevel level = projectService.getUserProjectRole(projectId, user.getId());
            if (level != ProjectRoleLevel.OWNER) {
                throw new CustomerException("无权限管理项目任务");
            }
        }
    }

    private void validateTaskFields(Task task) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new CustomerException("任务标题不能为空");
        }
        if (task.getProjectId() == null) {
            throw new CustomerException("项目ID不能为空");
        }
        if (task.getStartDate() != null && task.getEndDate() != null && task.getEndDate().isBefore(task.getStartDate())) {
            throw new CustomerException("结束日期不能早于开始日期");
        }
        if (task.getReviewerId() != null && task.getAssigneeId() != null
                && task.getReviewerId().equals(task.getAssigneeId())) {
            throw new CustomerException("审核人不能是任务组长自己");
        }
    }

    private void checkProjectWritable(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project != null && ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结，禁止写入操作");
        }
    }

    private void notifyAssigneeChanged(Task task, Integer newAssigneeId) {
        String url = "/project/" + task.getProjectId() + "/board";
        notificationService.send(newAssigneeId, "ASSIGN", "任务分配提醒",
                String.format("你被分配为任务「%s」的组长", task.getTitle()), url);
        notificationWebSocketService.sendStatsUpdate(newAssigneeId);
        if (task.getCreatorId() != null && !task.getCreatorId().equals(newAssigneeId)) {
            notificationWebSocketService.sendStatsUpdate(task.getCreatorId());
        }
    }

    private List<String> expandBlockedStatus(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return statuses;
        }
        List<String> result = new ArrayList<>(statuses);
        if (result.contains(TaskConstants.STATUS_IN_PROGRESS) && !result.contains(TaskConstants.STATUS_BLOCKED)) {
            result.add(TaskConstants.STATUS_BLOCKED);
        }
        return result;
    }

    private void fillTaskInfoBatch(List<Task> tasks, boolean withComments) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Integer> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());

        Set<Integer> userIds = new HashSet<>();
        for (Task task : tasks) {
            if (task.getAssigneeId() != null) userIds.add(task.getAssigneeId());
            if (task.getCreatorId() != null) userIds.add(task.getCreatorId());
            if (task.getReviewerId() != null) userIds.add(task.getReviewerId());
        }
        Map<Integer, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectList(
                    Wrappers.lambdaQuery(User.class)
                            .in(User::getId, userIds)
                            .eq(User::getDeleted, 0))
                    .stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        }

        List<TaskDependency> dependencies = taskDependencyService.selectDependsOnByTaskIds(taskIds);
        Map<Integer, List<Integer>> depIdMap = dependencies.stream()
                .collect(Collectors.groupingBy(TaskDependency::getTaskId,
                        Collectors.mapping(TaskDependency::getDependsOnTaskId, Collectors.toList())));

        Set<Integer> allDepIds = dependencies.stream()
                .map(TaskDependency::getDependsOnTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Task> depTaskMap = new HashMap<>();
        if (!allDepIds.isEmpty()) {
            depTaskMap = taskMapper.selectList(
                    Wrappers.lambdaQuery(Task.class)
                            .in(Task::getId, allDepIds)
                            .eq(Task::getDeleted, 0))
                    .stream()
                    .collect(Collectors.toMap(Task::getId, t -> t));
        }

        Map<Integer, List<TaskComment>> commentMap = new HashMap<>();
        if (withComments) {
            List<TaskComment> allComments = taskCommentMapper.selectByTaskIds(taskIds);
            for (TaskComment comment : allComments) {
                commentMap.computeIfAbsent(comment.getTaskId(), k -> new ArrayList<>()).add(comment);
            }
        }

        for (Task task : tasks) {
            if (task.getAssigneeName() == null && task.getAssigneeId() != null) {
                User u = userMap.get(task.getAssigneeId());
                task.setAssigneeName(u == null ? "" : u.getNickname());
            }
            if (task.getCreatorName() == null && task.getCreatorId() != null) {
                User u = userMap.get(task.getCreatorId());
                task.setCreatorName(u == null ? "" : u.getNickname());
            }
            if (task.getReviewerName() == null && task.getReviewerId() != null) {
                User u = userMap.get(task.getReviewerId());
                task.setReviewerName(u == null ? "" : u.getNickname());
            }
            task.setDependencyIds(depIdMap.getOrDefault(task.getId(), new ArrayList<>()));
            if (!task.getDependencyIds().isEmpty()) {
                task.setDependencies(task.getDependencyIds().stream()
                        .map(depTaskMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
            if (withComments) {
                task.setComments(taskCommentService.buildCommentTree(commentMap.getOrDefault(task.getId(), new ArrayList<>())));
            }
        }
    }

    private List<Task> buildTree(List<Task> tasks) {
        Map<Integer, Task> map = new LinkedHashMap<>();
        List<Task> roots = new ArrayList<>();
        for (Task task : tasks) {
            task.setChildren(new ArrayList<>());
            map.put(task.getId(), task);
        }
        for (Task task : tasks) {
            if (task.getParentTaskId() == null) {
                roots.add(task);
            } else {
                Task parent = map.get(task.getParentTaskId());
                if (parent != null) {
                    parent.getChildren().add(task);
                } else {
                    roots.add(task);
                }
            }
        }
        return roots;
    }
}