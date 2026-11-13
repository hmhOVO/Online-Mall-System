package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.ChangeTypeConstants;
import com.example.common.CurrentUserContext;
import com.example.common.LogActionConstants;
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
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TaskWorkflowService {

    private final TaskMapper taskMapper;
    private final TaskChangeLogMapper taskChangeLogMapper;
    private final TaskDependencyMapper taskDependencyMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final ProjectOperationLogService projectOperationLogService;
    private final NotificationWebSocketService notificationWebSocketService;
    private final ProjectService projectService;
    private final GroupMemberMapper groupMemberMapper;

    public TaskWorkflowService(TaskMapper taskMapper,
                              TaskChangeLogMapper taskChangeLogMapper,
                              TaskDependencyMapper taskDependencyMapper,
                              ProjectMapper projectMapper,
                              ProjectMemberMapper projectMemberMapper,
                              UserMapper userMapper,
                              NotificationService notificationService,
                              ProjectOperationLogService projectOperationLogService,
                              NotificationWebSocketService notificationWebSocketService,
                              ProjectService projectService,
                              GroupMemberMapper groupMemberMapper) {
        this.taskMapper = taskMapper;
        this.taskChangeLogMapper = taskChangeLogMapper;
        this.taskDependencyMapper = taskDependencyMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.projectOperationLogService = projectOperationLogService;
        this.notificationWebSocketService = notificationWebSocketService;
        this.projectService = projectService;
        this.groupMemberMapper = groupMemberMapper;
    }

    @Transactional
    public void claimTask(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (!RoleEnum.STUDENT.name().equals(current.getRole())) {
            throw new CustomerException("仅学生可以认领任务");
        }
        if (task.getAssigneeId() != null) {
            throw new CustomerException("该任务已被认领");
        }
        if (!TaskConstants.STATUS_TODO.equals(task.getStatus())) {
            throw new CustomerException("仅待办任务可被认领");
        }
        if (task.getGroupId() != null) {
            Integer myGroupId = groupMemberMapper.selectGroupIdByStudent(task.getProjectId(), current.getId());
            if (!task.getGroupId().equals(myGroupId)) {
                throw new CustomerException("该任务仅限指定小组的成员认领");
            }
        }

        Integer defaultReviewerId = recommendReviewer(task.getProjectId(), current.getId());

        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id)
                .set(Task::getAssigneeId, current.getId());
        if (defaultReviewerId != null) {
            wrapper.set(Task::getReviewerId, defaultReviewerId);
        }
        taskMapper.update(null, wrapper);

        saveProjectLog(task.getProjectId(), current.getId(), LogActionConstants.CLAIM_TASK,
                String.format("%s 认领了任务「%s」", current.getNickname() != null ? current.getNickname() : current.getUsername(), task.getTitle()));

        if (task.getCreatorId() != null && !task.getCreatorId().equals(current.getId())) {
            String url = "/project/" + task.getProjectId() + "/board";
            notificationService.send(task.getCreatorId(), "TASK_CLAIM", "任务被认领",
                    String.format("任务「%s」已被 %s 认领", task.getTitle(),
                            current.getNickname() != null ? current.getNickname() : current.getUsername()), url);
            notificationWebSocketService.sendStatsUpdate(task.getCreatorId());
        }
        notificationWebSocketService.sendStatsUpdate(current.getId());

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.ASSIGN);
    }

    @Transactional
    public void updateStatus(Integer id, String newStatus) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        if (TaskConstants.STATUS_BLOCKED.equals(task.getStatus()) && !TaskConstants.STATUS_IN_PROGRESS.equals(newStatus)) {
            throw new CustomerException("任务处于阻塞状态，需先解除阻塞");
        }
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (task.getAssigneeId() == null) {
            throw new CustomerException("任务尚未被认领，无法变更状态");
        }
        if (TaskConstants.STATUS_TODO.equals(task.getStatus()) && TaskConstants.STATUS_IN_PROGRESS.equals(newStatus)) {
            if (!task.getAssigneeId().equals(current.getId())) {
                throw new CustomerException("待办任务只能由负责人自行开始");
            }
        } else {
            if (!task.getAssigneeId().equals(current.getId()) && !canManageTask(task, current)) {
                throw new CustomerException("仅任务负责人、小组组长或指导教师可变更任务状态");
            }
        }
        boolean isBlockTransition = TaskConstants.STATUS_BLOCKED.equals(newStatus)
                || TaskConstants.STATUS_BLOCKED.equals(task.getStatus());
        if (isBlockTransition) {
            boolean canBlock = task.getAssigneeId().equals(current.getId())
                    || isProjectTeacher(task.getProjectId(), current)
                    || isGroupLeaderOf(task.getProjectId(), current.getId(), task.getAssigneeId());
            if (!canBlock) {
                throw new CustomerException("仅任务负责人、小组组长或指导教师可标记/解除阻塞");
            }
        }
        if (TaskConstants.STATUS_REVIEW.equals(task.getStatus())) {
            throw new CustomerException("审核中的任务请使用审核接口进行处理");
        }
        validateStatusTransition(task.getStatus(), newStatus);
        if (TaskConstants.STATUS_REVIEW.equals(newStatus)) {
            throw new CustomerException("请使用提交审核接口进入审核状态");
        }
        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id).set(Task::getStatus, newStatus);
        if (TaskConstants.STATUS_DONE.equals(newStatus)) {
            wrapper.set(Task::getProgress, 100).set(Task::getCompletedAt, LocalDateTime.now());
        }
        taskMapper.update(null, wrapper);

        syncProjectStatus(task.getProjectId(), newStatus);

        notifyStatusChanged(task, newStatus);

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.STATUS);
    }

    @Transactional
    public void updateActualHours(Integer id, Double actualHours) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(current.getId())) {
            throw new CustomerException("仅任务组长可更新实际学时");
        }
        if (TaskConstants.STATUS_REVIEW.equals(task.getStatus()) || TaskConstants.STATUS_DONE.equals(task.getStatus())) {
            throw new CustomerException("审核中或已完成的任务不可修改实际学时");
        }
        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id).set(Task::getActualHours, actualHours);
        taskMapper.update(null, wrapper);

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, "UPDATE");
    }

    @Transactional
    public void updateProgress(Integer id, Integer progress) {
        if (progress == null || progress < 0 || progress > 100) {
            throw new CustomerException("进度范围应为 0-100");
        }
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        if (TaskConstants.STATUS_BLOCKED.equals(task.getStatus())) {
            throw new CustomerException("任务处于阻塞状态，无法更新进度");
        }
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (task.getAssigneeId() == null) {
            throw new CustomerException("任务尚未被认领，无法更新进度");
        }
        if (!task.getAssigneeId().equals(current.getId())) {
            throw new CustomerException("仅任务组长可更新进度");
        }
        if (TaskConstants.STATUS_DONE.equals(task.getStatus())) {
            throw new CustomerException("已完成的任务无法更新进度");
        }
        if (TaskConstants.STATUS_REVIEW.equals(task.getStatus())) {
            throw new CustomerException("审核中的任务无法更新进度");
        }
        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id).set(Task::getProgress, progress);
        if (progress > 0 && TaskConstants.STATUS_TODO.equals(task.getStatus())) {
            wrapper.set(Task::getStatus, TaskConstants.STATUS_IN_PROGRESS);
        }
        taskMapper.update(null, wrapper);

        if (task.getAssigneeId() != null) {
            notificationWebSocketService.sendStatsUpdate(task.getAssigneeId());
        }

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.PROGRESS);
    }

    @Transactional
    public void submitReview(Integer id, Integer reviewerId) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        if (TaskConstants.STATUS_BLOCKED.equals(task.getStatus())) {
            throw new CustomerException("任务处于阻塞状态，无法提交审核");
        }
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (task.getAssigneeId() == null) {
            throw new CustomerException("任务尚未被认领，无法提交审核");
        }
        if (!task.getAssigneeId().equals(current.getId())) {
            throw new CustomerException("仅组长可提交审核");
        }
        if (!TaskConstants.STATUS_IN_PROGRESS.equals(task.getStatus()) && !TaskConstants.STATUS_TODO.equals(task.getStatus())) {
            throw new CustomerException("当前状态不可提交审核");
        }
        ProjectRoleLevel roleLevel = projectService.getUserProjectRole(task.getProjectId(), current.getId());
        if (roleLevel == ProjectRoleLevel.OWNER) {
            Project project = projectMapper.selectById(task.getProjectId());
            boolean hasTeacher = project != null && project.getTeacherId() != null
                    && !project.getTeacherId().equals(current.getId());
            if (!hasTeacher) {
                throw new CustomerException("项目未指定指导教师，作为项目负责人您可以直接完成任务");
            }
        }
        List<Integer> dependsOnIds = taskDependencyMapper.selectDependsOnIds(task.getId());
        if (dependsOnIds != null && !dependsOnIds.isEmpty()) {
            LambdaQueryWrapper<Task> depWrapper = Wrappers.lambdaQuery();
            depWrapper.in(Task::getId, dependsOnIds)
                    .eq(Task::getDeleted, 0)
                    .ne(Task::getStatus, TaskConstants.STATUS_DONE);
            long uncompleted = taskMapper.selectCount(depWrapper);
            if (uncompleted > 0) {
                List<Task> uncompletedDeps = taskMapper.selectList(depWrapper);
                List<String> titles = uncompletedDeps.stream()
                        .map(Task::getTitle)
                        .toList();
                throw new CustomerException("存在未完成的前置依赖任务，无法提交审核：" + String.join("、", titles));
            }
        }
        if (reviewerId != null) {
            task.setReviewerId(reviewerId);
        }
        if (task.getReviewerId() == null) {
            Integer recommendedReviewer = recommendReviewer(task.getProjectId(), task.getAssigneeId());
            if (recommendedReviewer == null) {
                throw new CustomerException("请先指定审核人");
            }
            task.setReviewerId(recommendedReviewer);
        }
        if (task.getActualHours() == null || task.getActualHours() <= 0) {
            throw new CustomerException("请填写实际学时");
        }

        if (task.getReviewerId().equals(current.getId())) {
            throw new CustomerException("不能选择自己作为审核人");
        }

        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id)
                .set(Task::getStatus, TaskConstants.STATUS_REVIEW)
                .set(Task::getProgress, 100)
                .set(Task::getReviewerId, task.getReviewerId());
        taskMapper.update(null, wrapper);

        if (task.getReviewerId() != null) {
            String url = "/project/" + task.getProjectId() + "/board";
            notificationService.send(task.getReviewerId(), "TASK_STATUS", "任务待审核",
                    String.format("任务「%s」已提交审核", task.getTitle()), url);
            notificationWebSocketService.sendStatsUpdate(task.getReviewerId());
        }
        if (task.getAssigneeId() != null) {
            notificationWebSocketService.sendStatsUpdate(task.getAssigneeId());
        }

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.REVIEW);
    }

    @Transactional
    public void approveReview(Integer id, Double score) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (!TaskConstants.STATUS_REVIEW.equals(task.getStatus())) {
            throw new CustomerException("当前任务不在审核中");
        }
        if (!canManageTask(task, current)) {
            throw new CustomerException("仅任务负责人所在小组的组长或指导教师可审核");
        }
        if (task.getAssigneeId() != null && task.getAssigneeId().equals(current.getId())) {
            throw new CustomerException("不能审核自己负责的任务");
        }
        // 组长为学生身份，只做组内验收；评分权归指导教师
        if (!isProjectTeacher(task.getProjectId(), current)) {
            score = null;
        }
        if (score != null && (score < 0 || score > 100)) {
            throw new CustomerException("评分应为 0-100 之间的数值");
        }
        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id)
                .set(Task::getStatus, TaskConstants.STATUS_DONE)
                .set(Task::getProgress, 100)
                .set(Task::getCompletedAt, LocalDateTime.now())
                .set(score != null, Task::getScore, score);
        taskMapper.update(null, wrapper);

        syncProjectStatus(task.getProjectId(), TaskConstants.STATUS_DONE);

        String url = "/project/" + task.getProjectId() + "/board";
        if (score != null) {
            notificationService.send(task.getAssigneeId(), "TASK_SCORED", "任务已评分",
                    String.format("任务「%s」审核已通过，评分：%s", task.getTitle(), score), url);
        } else {
            notificationService.send(task.getAssigneeId(), "TASK_STATUS", "任务审核通过",
                    String.format("任务「%s」审核已通过", task.getTitle()), url);
        }
        notificationWebSocketService.sendStatsUpdate(task.getAssigneeId());

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.REVIEW);
    }

    /**
     * 教师评分：对已完成的任务打分/改分，组长无评分权。
     */
    @Transactional
    public void scoreTask(Integer id, Double score) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        if (!isProjectTeacher(task.getProjectId(), current)) {
            throw new CustomerException("仅指导教师可评分");
        }
        if (!TaskConstants.STATUS_DONE.equals(task.getStatus())) {
            throw new CustomerException("仅已完成的任务可评分");
        }
        if (score == null || score < 0 || score > 100) {
            throw new CustomerException("评分应为 0-100 之间的数值");
        }
        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id).set(Task::getScore, score);
        taskMapper.update(null, wrapper);

        if (task.getAssigneeId() != null) {
            notificationService.send(task.getAssigneeId(), "TASK_SCORED", "任务已评分",
                    String.format("任务「%s」已评分：%s", task.getTitle(), score),
                    "/project/" + task.getProjectId() + "/board");
            notificationWebSocketService.sendStatsUpdate(task.getAssigneeId());
        }

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.REVIEW, "评分：" + score);
    }

    @Transactional
    public void rejectReview(Integer id, String comment) {
        User current = CurrentUserContext.get();
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (!TaskConstants.STATUS_REVIEW.equals(task.getStatus())) {
            throw new CustomerException("当前任务不在审核中");
        }
        if (!canManageTask(task, current)) {
            throw new CustomerException("仅任务负责人所在小组的组长或指导教师可审核");
        }
        if (task.getAssigneeId() != null && task.getAssigneeId().equals(current.getId())) {
            throw new CustomerException("不能审核自己负责的任务");
        }
        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id)
                .set(Task::getStatus, TaskConstants.STATUS_IN_PROGRESS)
                .set(Task::getReviewComment, comment);
        taskMapper.update(null, wrapper);

        String url = "/project/" + task.getProjectId() + "/board";
        notificationService.send(task.getAssigneeId(), "REJECT", "任务审核被驳回",
                String.format("任务「%s」审核被驳回%s", task.getTitle(),
                        comment == null || comment.isBlank() ? "" : "，原因：" + comment), url);
        notificationWebSocketService.sendStatsUpdate(task.getAssigneeId());

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.REVIEW, comment);
    }

    @Transactional
    public void withdrawReview(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (!TaskConstants.STATUS_REVIEW.equals(task.getStatus())) {
            throw new CustomerException("当前任务不在审核中，无法撤回");
        }
        boolean canWithdraw = task.getAssigneeId() != null && task.getAssigneeId().equals(current.getId())
                || isProjectTeacher(task.getProjectId(), current)
                || isGroupLeaderOf(task.getProjectId(), current.getId(), task.getAssigneeId());
        if (!canWithdraw) {
            throw new CustomerException("仅任务负责人、小组组长或指导教师可撤回审核");
        }

        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id)
                .set(Task::getStatus, TaskConstants.STATUS_IN_PROGRESS)
                .set(Task::getReviewComment, null);
        taskMapper.update(null, wrapper);

        String operatorRole = task.getAssigneeId() != null && task.getAssigneeId().equals(current.getId())
                ? "任务负责人"
                : (isProjectTeacher(task.getProjectId(), current) ? "指导教师" : "小组组长");
        String url = "/project/" + task.getProjectId() + "/board";
        if (task.getReviewerId() != null) {
            notificationService.send(task.getReviewerId(), "REJECT", "任务审核已撤回",
                    String.format("任务「%s」的审核已被%s撤回", task.getTitle(), operatorRole), url);
            notificationWebSocketService.sendStatsUpdate(task.getReviewerId());
        }
        notificationWebSocketService.sendStatsUpdate(task.getAssigneeId());

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.REVIEW, operatorRole + "撤回审核");
    }

    @Transactional
    public void completeDirectly(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        Task task = getTask(id);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(current.getId())) {
            throw new CustomerException("仅任务负责人可直接完成");
        }
        Project project = projectMapper.selectById(task.getProjectId());
        if (project != null && project.getTeacherId() != null && !project.getTeacherId().equals(current.getId())) {
            throw new CustomerException("项目已指定指导教师，请走正常审核流程");
        }
        if (!TaskConstants.STATUS_IN_PROGRESS.equals(task.getStatus()) && !TaskConstants.STATUS_TODO.equals(task.getStatus())) {
            throw new CustomerException("当前状态不可直接完成");
        }
        if (task.getActualHours() == null || task.getActualHours() <= 0) {
            throw new CustomerException("请填写实际学时");
        }

        LambdaUpdateWrapper<Task> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Task::getId, id)
                .set(Task::getStatus, TaskConstants.STATUS_DONE)
                .set(Task::getProgress, 100)
                .set(Task::getCompletedAt, LocalDateTime.now())
                .set(Task::getReviewerId, current.getId())
                .set(Task::getReviewComment, "无指导教师，负责人直接完成");
        taskMapper.update(null, wrapper);

        syncProjectStatus(task.getProjectId(), TaskConstants.STATUS_DONE);

        Task updatedTask = taskMapper.selectById(id);
        logTaskChanges(task, updatedTask, current, ChangeTypeConstants.REVIEW, "无指导教师，负责人直接完成");
    }

    /**
     * 审核路由：组员任务由所在小组的组长审核；小组组长本人的任务或无组长时由指导教师审核。
     */
    private Integer recommendReviewer(Integer projectId, Integer assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        Project project = projectMapper.selectById(projectId);
        Integer leaderId = getGroupLeaderId(projectId, assigneeId);
        if (leaderId != null && !leaderId.equals(assigneeId)) {
            return leaderId;
        }
        if (project != null && project.getTeacherId() != null && !project.getTeacherId().equals(assigneeId)) {
            return project.getTeacherId();
        }
        return null;
    }

    private Integer getGroupLeaderId(Integer projectId, Integer studentId) {
        if (studentId == null) {
            return null;
        }
        return groupMemberMapper.selectLeaderIdByStudent(projectId, studentId);
    }

    private Task getTask(Integer id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new CustomerException("任务不存在");
        }
        return task;
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

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (Objects.equals(currentStatus, newStatus)) {
            return;
        }
        switch (currentStatus) {
            case TaskConstants.STATUS_TODO:
                if (TaskConstants.STATUS_IN_PROGRESS.equals(newStatus)) {
                    return;
                }
                throw new CustomerException("TODO 状态只能流转到 IN_PROGRESS");
            case TaskConstants.STATUS_IN_PROGRESS:
                if (TaskConstants.STATUS_REVIEW.equals(newStatus)
                        || TaskConstants.STATUS_DONE.equals(newStatus)
                        || TaskConstants.STATUS_BLOCKED.equals(newStatus)) {
                    return;
                }
                throw new CustomerException("IN_PROGRESS 状态只能流转到 REVIEW、DONE 或 BLOCKED");
            case TaskConstants.STATUS_BLOCKED:
                if (TaskConstants.STATUS_IN_PROGRESS.equals(newStatus)) {
                    return;
                }
                throw new CustomerException("已阻塞状态只能流转到 IN_PROGRESS");
            case TaskConstants.STATUS_REVIEW:
                throw new CustomerException("审核中的任务请使用审核接口进行处理");
            case TaskConstants.STATUS_DONE:
                throw new CustomerException("已完成的任务不能修改状态");
            default:
                throw new CustomerException("未知状态：" + currentStatus);
        }
    }

    private boolean canManageTask(Task task, User current) {
        if (isProjectTeacher(task.getProjectId(), current)) {
            return true;
        }
        if (task.getReviewerId() != null && task.getReviewerId().equals(current.getId())) {
            return true;
        }
        return isGroupLeaderOf(task.getProjectId(), current.getId(), task.getAssigneeId());
    }

    private boolean isProjectTeacher(Integer projectId, User user) {
        Project project = projectMapper.selectById(projectId);
        return project != null && Objects.equals(project.getTeacherId(), user.getId());
    }

    /**
     * 判断 leaderId 是否为 studentId 所在小组的组长。
     */
    private boolean isGroupLeaderOf(Integer projectId, Integer leaderId, Integer studentId) {
        if (leaderId == null || studentId == null || leaderId.equals(studentId)) {
            return false;
        }
        Integer leader = groupMemberMapper.selectLeaderIdByStudent(projectId, studentId);
        return leader != null && leader.equals(leaderId);
    }

    private void checkProjectWritable(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project != null && ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结，禁止写入操作");
        }
    }

    private void notifyStatusChanged(Task task, String newStatus) {
        if (task.getAssigneeId() == null) return;
        String statusText = switch (newStatus) {
            case TaskConstants.STATUS_TODO -> "待办";
            case TaskConstants.STATUS_IN_PROGRESS -> "进行中";
            case TaskConstants.STATUS_DONE -> "已完成";
            default -> newStatus;
        };
        String url = "/project/" + task.getProjectId() + "/board";
        notificationService.send(task.getAssigneeId(), "TASK_STATUS", "任务状态变更",
                String.format("任务「%s」状态变更为「%s」", task.getTitle(), statusText), url);
        notificationWebSocketService.sendStatsUpdate(task.getAssigneeId());
    }

    private void syncProjectStatus(Integer projectId, String taskStatus) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) return;

        if (ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            return;
        }

        if (TaskConstants.STATUS_DONE.equals(taskStatus)) {
            if (ProjectConstants.PROJECT_STATUS_NOT_STARTED.equals(project.getStatus())) {
                LambdaUpdateWrapper<Project> wrapper = Wrappers.lambdaUpdate();
                wrapper.eq(Project::getId, projectId)
                        .set(Project::getStatus, ProjectConstants.PROJECT_STATUS_IN_PROGRESS);
                projectMapper.update(null, wrapper);
            }

            LambdaQueryWrapper<Task> taskWrapper = Wrappers.lambdaQuery();
            taskWrapper.eq(Task::getProjectId, projectId)
                    .eq(Task::getDeleted, 0)
                    .ne(Task::getStatus, TaskConstants.STATUS_DONE);
            long notDoneCount = taskMapper.selectCount(taskWrapper);

            if (notDoneCount == 0) {
                LambdaUpdateWrapper<Project> wrapper = Wrappers.lambdaUpdate();
                wrapper.eq(Project::getId, projectId)
                        .set(Project::getStatus, ProjectConstants.PROJECT_STATUS_COMPLETED);
                projectMapper.update(null, wrapper);
            }
        }
    }

    private void saveProjectLog(Integer projectId, Integer operatorId, String action, String detail) {
        User current = CurrentUserContext.get();
        projectOperationLogService.saveLog(projectId, operatorId,
                current == null ? null : current.getUsername(), action, detail);
    }

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("title", "任务标题"),
            Map.entry("description", "任务描述"),
            Map.entry("priority", "优先级"),
            Map.entry("status", "状态"),
            Map.entry("progress", "进度"),
            Map.entry("plannedHours", "计划学时"),
            Map.entry("actualHours", "实际学时"),
            Map.entry("startDate", "开始日期"),
            Map.entry("endDate", "截止日期"),
            Map.entry("assigneeId", "负责人"),
            Map.entry("reviewerId", "审核人"),
            Map.entry("reviewComment", "审核意见"),
            Map.entry("score", "评分"),
            Map.entry("taskType", "任务类型"),
            Map.entry("difficulty", "难度")
    );

    private static final Map<String, String> STATUS_LABELS = Map.ofEntries(
            Map.entry(TaskConstants.STATUS_TODO, "待办"),
            Map.entry(TaskConstants.STATUS_IN_PROGRESS, "进行中"),
            Map.entry(TaskConstants.STATUS_BLOCKED, "已阻塞"),
            Map.entry(TaskConstants.STATUS_REVIEW, "审核中"),
            Map.entry(TaskConstants.STATUS_DONE, "已完成"),
            Map.entry(TaskConstants.PRIORITY_HIGH, "高"),
            Map.entry(TaskConstants.PRIORITY_MEDIUM, "中"),
            Map.entry(TaskConstants.PRIORITY_LOW, "低")
    );

    private void logTaskChanges(Task oldTask, Task newTask, User changedBy, String changeType) {
        logTaskChanges(oldTask, newTask, changedBy, changeType, null);
    }

    private void logTaskChanges(Task oldTask, Task newTask, User changedBy, String changeType, String comment) {
        List<TaskChangeLog> logs = new ArrayList<>();

        addFieldChange(logs, oldTask.getTitle(), newTask.getTitle(), "title", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getDescription(), newTask.getDescription(), "description", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getPriority(), newTask.getPriority(), "priority", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getStatus(), newTask.getStatus(), "status", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getProgress(), newTask.getProgress(), "progress", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getPlannedHours(), newTask.getPlannedHours(), "plannedHours", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getActualHours(), newTask.getActualHours(), "actualHours", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getStartDate(), newTask.getStartDate(), "startDate", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getEndDate(), newTask.getEndDate(), "endDate", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getAssigneeId(), newTask.getAssigneeId(), "assigneeId", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getReviewerId(), newTask.getReviewerId(), "reviewerId", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getReviewComment(), newTask.getReviewComment(), "reviewComment", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getScore(), newTask.getScore(), "score", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getTaskType(), newTask.getTaskType(), "taskType", changedBy.getId(), changeType, comment);
        addFieldChange(logs, oldTask.getDifficulty(), newTask.getDifficulty(), "difficulty", changedBy.getId(), changeType, comment);

        if (!logs.isEmpty()) {
            for (TaskChangeLog log : logs) {
                log.setTaskId(oldTask.getId());
                log.setChangedAt(LocalDateTime.now());
                taskChangeLogMapper.insert(log);
            }
        }
    }

    private void addFieldChange(List<TaskChangeLog> logs, Object oldValue, Object newValue, String fieldName, Integer changedBy, String changeType, String comment) {
        String oldStr = formatValue(oldValue);
        String newStr = formatValue(newValue);
        if (!Objects.equals(oldStr, newStr)) {
            TaskChangeLog log = new TaskChangeLog();
            log.setFieldName(fieldName);
            log.setFieldLabel(FIELD_LABELS.getOrDefault(fieldName, fieldName));
            log.setOldValue(oldStr);
            log.setNewValue(newStr);
            log.setChangedBy(changedBy);
            log.setChangeType(changeType);
            log.setComment(comment);
            logs.add(log);
        }
    }

    private String formatValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toString();
        }
        String strValue = value.toString();
        return STATUS_LABELS.getOrDefault(strValue, strValue);
    }
}