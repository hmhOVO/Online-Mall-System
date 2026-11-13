package com.example.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.CurrentUserContext;
import com.example.common.ProjectConstants;
import com.example.entity.GroupMember;
import com.example.entity.GroupMessage;
import com.example.entity.Project;
import com.example.entity.StudentGroup;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.GroupMemberMapper;
import com.example.mapper.GroupMessageMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.StudentGroupMapper;
import com.example.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GroupMessageService {

    private final GroupMessageMapper groupMessageMapper;
    private final StudentGroupMapper studentGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public GroupMessageService(GroupMessageMapper groupMessageMapper,
                               StudentGroupMapper studentGroupMapper,
                               GroupMemberMapper groupMemberMapper,
                               ProjectMapper projectMapper,
                               UserMapper userMapper,
                               NotificationService notificationService) {
        this.groupMessageMapper = groupMessageMapper;
        this.studentGroupMapper = studentGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.projectMapper = projectMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    public List<GroupMessage> list(Integer groupId) {
        StudentGroup group = getGroup(groupId);
        User current = CurrentUserContext.get();
        validateAccess(group, current);
        List<GroupMessage> messages = groupMessageMapper.selectRecentByGroupId(groupId, 100);
        Collections.reverse(messages);
        for (GroupMessage message : messages) {
            message.setIsLeader(Objects.equals(message.getSenderId(), group.getLeaderId()));
        }
        return messages;
    }

    @Transactional
    public GroupMessage send(Integer groupId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new CustomerException("发言内容不能为空");
        }
        String text = content.trim();
        if (text.length() > 500) {
            throw new CustomerException("发言内容不能超过 500 字");
        }
        StudentGroup group = getGroup(groupId);
        User current = CurrentUserContext.get();
        validateAccess(group, current);
        Project project = projectMapper.selectById(group.getProjectId());
        if (project != null && ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结，禁止发言");
        }

        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(current.getId());
        message.setContent(text);
        groupMessageMapper.insert(message);

        sendMentionNotifications(group, current, text);
        return message;
    }

    private StudentGroup getGroup(Integer groupId) {
        StudentGroup group = studentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomerException("小组不存在");
        }
        return group;
    }

    private void validateAccess(StudentGroup group, User current) {
        Project project = projectMapper.selectById(group.getProjectId());
        if (project == null) {
            throw new CustomerException("项目不存在");
        }
        boolean isAdmin = RoleEnum.ADMIN.name().equals(current.getRole());
        boolean isTeacher = Objects.equals(project.getTeacherId(), current.getId());
        boolean isOwner = project.getOwnerId() != null && project.getOwnerId().equals(current.getId());
        if (isAdmin || isTeacher || isOwner) {
            return;
        }
        long count = groupMemberMapper.selectCount(Wrappers.lambdaQuery(GroupMember.class)
                .eq(GroupMember::getGroupId, group.getId())
                .eq(GroupMember::getStudentId, current.getId()));
        if (count == 0) {
            throw new CustomerException("你不是该小组成员，无法访问小组讨论");
        }
    }

    private void sendMentionNotifications(StudentGroup group, User current, String content) {
        Set<String> usernames = extractMentions(content);
        if (usernames.isEmpty()) {
            return;
        }
        List<GroupMember> members = groupMemberMapper.selectList(Wrappers.lambdaQuery(GroupMember.class)
                .eq(GroupMember::getGroupId, group.getId()));
        Set<Integer> memberIds = members.stream()
                .map(GroupMember::getStudentId)
                .collect(Collectors.toSet());
        String senderName = current.getNickname() != null && !current.getNickname().isEmpty()
                ? current.getNickname()
                : current.getUsername();
        String groupName = group.getName() != null && !group.getName().isBlank()
                ? group.getName()
                : "小组讨论";
        List<Integer> targets = new ArrayList<>();
        for (String username : usernames) {
            User target = userMapper.selectByUsername(username);
            if (target == null || target.getId().equals(current.getId()) || !memberIds.contains(target.getId())) {
                continue;
            }
            if (!targets.contains(target.getId())) {
                targets.add(target.getId());
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        notificationService.sendBatch(targets, "MENTION", "小组讨论中有人@你",
                senderName + " 在「" + groupName + "」的讨论中@了你", "/my-group");
    }

    private Set<String> extractMentions(String content) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("@([^\\s@]+)").matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
