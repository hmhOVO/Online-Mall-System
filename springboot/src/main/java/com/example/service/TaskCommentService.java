package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.CurrentUserContext;
import com.example.common.ProjectConstants;
import com.example.common.ProjectRoleLevel;
import com.example.entity.*;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TaskCommentService {

    private final TaskCommentMapper taskCommentMapper;
    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final NotificationWebSocketService notificationWebSocketService;
    private final ProjectService projectService;

    public TaskCommentService(TaskCommentMapper taskCommentMapper,
                              TaskMapper taskMapper,
                              ProjectMapper projectMapper,
                              ProjectMemberMapper projectMemberMapper,
                              UserMapper userMapper,
                              NotificationService notificationService,
                              NotificationWebSocketService notificationWebSocketService,
                              ProjectService projectService) {
        this.taskCommentMapper = taskCommentMapper;
        this.taskMapper = taskMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.notificationWebSocketService = notificationWebSocketService;
        this.projectService = projectService;
    }

    @Transactional
    public void addComment(Integer taskId, Integer parentCommentId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new CustomerException("评论内容不能为空");
        }
        User current = CurrentUserContext.get();
        Task task = getTask(taskId);
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(current.getId());
        comment.setParentCommentId(parentCommentId);
        comment.setContent(content.trim());
        comment.setIsResolved(false);
        comment.setCreatedAt(LocalDateTime.now());
        taskCommentMapper.insert(comment);
        sendCommentNotifications(task, current, comment);
    }

    @Transactional
    public void resolveComment(Integer commentId, Boolean resolved) {
        User current = CurrentUserContext.get();
        TaskComment comment = taskCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new CustomerException("评论不存在");
        }
        Task task = getTask(comment.getTaskId());
        validateProjectAccess(task.getProjectId(), current, true);
        checkProjectWritable(task.getProjectId());
        if (!canManageTask(task, current)) {
            throw new CustomerException("仅项目管理者可标记评论");
        }
        LambdaUpdateWrapper<TaskComment> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(TaskComment::getId, commentId).set(TaskComment::getIsResolved, resolved);
        taskCommentMapper.update(null, wrapper);
    }

    @Transactional
    public void updateComment(Integer commentId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new CustomerException("评论内容不能为空");
        }
        User current = CurrentUserContext.get();
        TaskComment comment = taskCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new CustomerException("评论不存在");
        }
        if (!comment.getUserId().equals(current.getId())) {
            throw new CustomerException("仅评论作者可编辑评论");
        }
        Task task = getTask(comment.getTaskId());
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        LambdaUpdateWrapper<TaskComment> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(TaskComment::getId, commentId)
                .set(TaskComment::getContent, content.trim())
                .set(TaskComment::getEditedAt, LocalDateTime.now());
        taskCommentMapper.update(null, wrapper);
    }

    @Transactional
    public void deleteComment(Integer commentId) {
        User current = CurrentUserContext.get();
        TaskComment comment = taskCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new CustomerException("评论不存在");
        }
        Task task = getTask(comment.getTaskId());
        validateProjectAccess(task.getProjectId(), current, false);
        checkProjectWritable(task.getProjectId());
        if (!comment.getUserId().equals(current.getId()) && !canManageTask(task, current)) {
            throw new CustomerException("仅评论作者或项目管理者可删除评论");
        }
        taskCommentMapper.deleteById(commentId);
    }

    public List<TaskComment> getTaskComments(Integer taskId) {
        Task task = getTask(taskId);
        User current = CurrentUserContext.get();
        validateProjectAccess(task.getProjectId(), current, false);
        List<TaskComment> comments = taskCommentMapper.selectByTaskId(taskId);
        return buildCommentTree(comments);
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

    private void checkProjectWritable(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project != null && ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结，禁止写入操作");
        }
    }

    private boolean canManageTask(Task task, User current) {
        ProjectRoleLevel level = projectService.getUserProjectRole(task.getProjectId(), current.getId());
        if (level == ProjectRoleLevel.OWNER) {
            return true;
        }
        return task.getReviewerId() != null && task.getReviewerId().equals(current.getId());
    }

    private void sendCommentNotifications(Task task, User current, TaskComment comment) {
        String senderName = current.getNickname() != null && !current.getNickname().isEmpty()
                ? current.getNickname()
                : current.getUsername();
        String targetUrl = "/project/" + task.getProjectId() + "/board";

        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(current.getId())) {
            String title = "任务有新评论";
            String notifyContent = senderName + " 在任务《" + task.getTitle() + "》中发表了评论";
            notificationService.send(task.getAssigneeId(), "TASK_COMMENT", title, notifyContent, targetUrl);
        }

        if (task.getCreatorId() != null && !task.getCreatorId().equals(current.getId())
                && !task.getCreatorId().equals(task.getAssigneeId())) {
            String title = "任务有新评论";
            String notifyContent = senderName + " 在任务《" + task.getTitle() + "》中发表了评论";
            notificationService.send(task.getCreatorId(), "TASK_COMMENT", title, notifyContent, targetUrl);
        }

        sendMentionNotifications(task, current, comment);
    }

    private void sendMentionNotifications(Task task, User current, TaskComment comment) {
        Set<String> usernames = extractMentions(comment.getContent());
        if (usernames.isEmpty()) {
            return;
        }
        List<ProjectMember> members = projectMemberMapper.selectByProjectId(task.getProjectId());
        Set<Integer> memberUserIds = members.stream()
                .map(ProjectMember::getUserId)
                .collect(Collectors.toSet());
        String senderName = current.getNickname() != null && !current.getNickname().isEmpty()
                ? current.getNickname()
                : current.getUsername();
        String targetUrl = "/project/" + task.getProjectId() + "/board";
        for (String username : usernames) {
            User target = userMapper.selectByUsername(username);
            if (target == null) {
                continue;
            }
            if (target.getId().equals(current.getId())) {
                continue;
            }
            if (!memberUserIds.contains(target.getId())) {
                continue;
            }
            String title = "任务评论中有人@你";
            String notifyContent = senderName + " 在任务《" + task.getTitle() + "》的评论中@了你";
            notificationService.send(target.getId(), "MENTION", title, notifyContent, targetUrl);
        }
    }

    private Set<String> extractMentions(String content) {
        Set<String> result = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("@([^\\s@]+)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    public List<TaskComment> buildCommentTree(List<TaskComment> comments) {
        Map<Integer, TaskComment> map = new LinkedHashMap<>();
        List<TaskComment> roots = new ArrayList<>();
        for (TaskComment comment : comments) {
            comment.setReplies(new ArrayList<>());
            map.put(comment.getId(), comment);
        }
        for (TaskComment comment : comments) {
            if (comment.getParentCommentId() == null) {
                roots.add(comment);
            } else {
                TaskComment parent = map.get(comment.getParentCommentId());
                if (parent != null) {
                    parent.getReplies().add(comment);
                } else {
                    roots.add(comment);
                }
            }
        }
        return roots;
    }
}