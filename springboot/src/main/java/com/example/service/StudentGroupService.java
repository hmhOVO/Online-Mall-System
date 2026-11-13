package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.CurrentUserContext;
import com.example.common.ProjectConstants;
import com.example.entity.GroupMember;
import com.example.entity.Project;
import com.example.entity.ProjectMember;
import com.example.entity.StudentGroup;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.GroupMemberMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ProjectMemberMapper;
import com.example.mapper.StudentGroupMapper;
import com.example.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentGroupService {

    private final StudentGroupMapper studentGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final NotificationService notificationService;

    public StudentGroupService(StudentGroupMapper studentGroupMapper,
                               GroupMemberMapper groupMemberMapper,
                               UserMapper userMapper,
                               ProjectMapper projectMapper,
                               ProjectMemberMapper projectMemberMapper,
                               NotificationService notificationService) {
        this.studentGroupMapper = studentGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.userMapper = userMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.notificationService = notificationService;
    }

    public List<StudentGroup> listByProject(Integer projectId) {
        User current = CurrentUserContext.get();
        List<StudentGroup> groups = studentGroupMapper.selectList(Wrappers.lambdaQuery(StudentGroup.class)
                .eq(StudentGroup::getProjectId, projectId)
                .orderByAsc(StudentGroup::getId));
        boolean isStudent = current != null && RoleEnum.STUDENT.name().equals(current.getRole());
        if (isStudent && projectMemberMapper.selectCount(Wrappers.lambdaQuery(ProjectMember.class)
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, current.getId())) == 0) {
            throw new CustomerException("你不是该项目成员，无法查看分组");
        }
        for (StudentGroup group : groups) {
            group.setMemberCount((int) countMembers(group.getId()));
            if (group.getLeaderId() != null) {
                User leader = userMapper.selectById(group.getLeaderId());
                if (leader != null) {
                    group.setLeaderName(leader.getNickname() != null ? leader.getNickname() : leader.getUsername());
                }
            }
            if (isStudent) {
                group.setJoinedByMe(groupMemberMapper.selectCount(Wrappers.lambdaQuery(GroupMember.class)
                        .eq(GroupMember::getGroupId, group.getId())
                        .eq(GroupMember::getStudentId, current.getId())) > 0);
            }
        }
        return groups;
    }

    public List<GroupMember> listMembers(Integer groupId) {
        List<GroupMember> members = groupMemberMapper.selectList(Wrappers.lambdaQuery(GroupMember.class)
                .eq(GroupMember::getGroupId, groupId)
                .orderByAsc(GroupMember::getJoinedAt));
        for (GroupMember member : members) {
            User user = userMapper.selectById(member.getStudentId());
            if (user != null) {
                member.setUsername(user.getUsername());
                member.setNickname(user.getNickname());
                member.setStudentNo(user.getStudentNo());
            }
        }
        return members;
    }

    public StudentGroup create(StudentGroup group) {
        if (group.getProjectId() == null) {
            throw new CustomerException("所属项目不能为空");
        }
        getWritableProject(group.getProjectId());
        if (group.getName() == null || group.getName().isBlank()) {
            throw new CustomerException("小组名称不能为空");
        }
        if (group.getMaxMembers() == null || group.getMaxMembers() < 0) {
            group.setMaxMembers(0);
        }
        if (group.getJoinEnabled() == null) {
            group.setJoinEnabled(true);
        }
        studentGroupMapper.insert(group);
        return group;
    }

    public void update(Integer id, StudentGroup group) {
        StudentGroup existing = studentGroupMapper.selectById(id);
        if (existing == null) {
            throw new CustomerException("分组不存在");
        }
        if (group.getMaxMembers() != null && group.getMaxMembers() < 0) {
            throw new CustomerException("人数上限不能为负数");
        }
        if (group.getMaxMembers() != null && group.getMaxMembers() > 0
                && group.getMaxMembers() < countMembers(id)) {
            throw new CustomerException("人数上限不能小于当前组员人数（" + countMembers(id) + "）");
        }
        group.setId(id);
        group.setProjectId(existing.getProjectId());
        studentGroupMapper.updateById(group);
    }

    /**
     * 学生修改自己所在小组的名称。
     */
    public void renameByMember(Integer groupId, String name) {
        StudentGroup group = studentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomerException("分组不存在");
        }
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.STUDENT.name().equals(current.getRole())) {
            throw new CustomerException("仅学生可修改小组名称");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new CustomerException("小组名称不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 50) {
            throw new CustomerException("小组名称不能超过 50 个字符");
        }
        Long memberCount = groupMemberMapper.selectCount(Wrappers.lambdaQuery(GroupMember.class)
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getStudentId, current.getId()));
        if (memberCount == null || memberCount == 0) {
            throw new CustomerException("仅本组成员可修改小组名称");
        }
        StudentGroup update = new StudentGroup();
        update.setId(groupId);
        update.setName(trimmed);
        studentGroupMapper.updateById(update);
    }

    @Transactional
    public void delete(Integer id) {
        if (studentGroupMapper.selectById(id) == null) {
            throw new CustomerException("分组不存在");
        }
        groupMemberMapper.delete(Wrappers.lambdaQuery(GroupMember.class)
                .eq(GroupMember::getGroupId, id));
        studentGroupMapper.physicalDeleteById(id);
    }

    /**
     * 批量创建分组，自动命名为「第N组」，跳过已存在的名称。
     */
    @Transactional
    public Map<String, Object> batchCreate(Integer projectId, Integer count, Integer maxMembers, Boolean joinEnabled) {
        getWritableProject(projectId);
        if (count == null || count < 1 || count > 50) {
            throw new CustomerException("一次可创建 1~50 个分组");
        }
        int limit = (maxMembers == null || maxMembers < 0) ? 0 : maxMembers;
        boolean allowJoin = joinEnabled == null || joinEnabled;
        Set<String> existingNames = new HashSet<>();
        for (StudentGroup group : studentGroupMapper.selectList(Wrappers.lambdaQuery(StudentGroup.class)
                .eq(StudentGroup::getProjectId, projectId))) {
            existingNames.add(group.getName());
        }
        int created = 0;
        int seq = 1;
        while (created < count) {
            String name = "第" + seq + "组";
            seq++;
            if (existingNames.contains(name)) {
                continue;
            }
            StudentGroup group = new StudentGroup();
            group.setProjectId(projectId);
            group.setName(name);
            group.setGroupNo(name);
            group.setMaxMembers(limit);
            group.setJoinEnabled(allowJoin);
            studentGroupMapper.insert(group);
            existingNames.add(name);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupCount", created);
        return result;
    }

    @Transactional
    public void addMembers(Integer groupId, List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new CustomerException("请选择要加入的学生");
        }
        StudentGroup group = studentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomerException("分组不存在");
        }
        for (Integer studentId : studentIds) {
            User student = userMapper.selectById(studentId);
            if (student == null || !RoleEnum.STUDENT.name().equals(student.getRole())) {
                throw new CustomerException("只能将学生加入小组，请检查所选成员身份");
            }
            if (projectMemberMapper.selectCount(Wrappers.lambdaQuery(ProjectMember.class)
                    .eq(ProjectMember::getProjectId, group.getProjectId())
                    .eq(ProjectMember::getUserId, studentId)) == 0) {
                throw new CustomerException("学生「" + displayName(student) + "」不是该项目成员，请先加入项目");
            }
            Integer joinedGroupId = groupMemberMapper.selectGroupIdByStudent(group.getProjectId(), studentId);
            if (joinedGroupId != null && !joinedGroupId.equals(groupId)) {
                throw new CustomerException("学生「" + displayName(student) + "」已在其他小组中，请先移出原小组");
            }
        }
        int currentCount = (int) countMembers(groupId);
        int toAdd = 0;
        for (Integer studentId : studentIds) {
            if (groupMemberMapper.selectCount(Wrappers.lambdaQuery(GroupMember.class)
                    .eq(GroupMember::getGroupId, groupId)
                    .eq(GroupMember::getStudentId, studentId)) == 0) {
                toAdd++;
            }
        }
        if (group.getMaxMembers() != null && group.getMaxMembers() > 0
                && currentCount + toAdd > group.getMaxMembers()) {
            throw new CustomerException("超出小组人数上限（" + group.getMaxMembers() + " 人）");
        }
        List<Integer> added = new ArrayList<>();
        for (Integer studentId : studentIds) {
            Long exists = groupMemberMapper.selectCount(Wrappers.lambdaQuery(GroupMember.class)
                    .eq(GroupMember::getGroupId, groupId)
                    .eq(GroupMember::getStudentId, studentId));
            if (exists > 0) {
                continue;
            }
            // 清理历史逻辑删除记录，避免唯一键冲突
            groupMemberMapper.physicalDeleteByGroupAndStudent(groupId, studentId);
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setStudentId(studentId);
            member.setJoinedAt(LocalDateTime.now());
            groupMemberMapper.insert(member);
            added.add(studentId);
        }
        notifyGroupInvite(group, added);
    }

    public void removeMember(Integer groupId, Integer studentId) {
        StudentGroup group = studentGroupMapper.selectById(groupId);
        groupMemberMapper.physicalDeleteByGroupAndStudent(groupId, studentId);
        if (group != null && studentId.equals(group.getLeaderId())) {
            studentGroupMapper.update(null, Wrappers.lambdaUpdate(StudentGroup.class)
                    .eq(StudentGroup::getId, groupId)
                    .set(StudentGroup::getLeaderId, null));
        }
    }

    /**
     * 学生自选加入项目内某个小组。
     */
    @Transactional
    public void studentJoin(Integer groupId) {
        User current = CurrentUserContext.get();
        if (current == null || !RoleEnum.STUDENT.name().equals(current.getRole())) {
            throw new CustomerException("仅学生可自行加入小组");
        }
        StudentGroup group = studentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomerException("分组不存在");
        }
        getWritableProject(group.getProjectId());
        if (!Boolean.TRUE.equals(group.getJoinEnabled())) {
            throw new CustomerException("该小组未开放自选加入，请联系指导教师");
        }
        if (projectMemberMapper.selectCount(Wrappers.lambdaQuery(ProjectMember.class)
                .eq(ProjectMember::getProjectId, group.getProjectId())
                .eq(ProjectMember::getUserId, current.getId())) == 0) {
            throw new CustomerException("你尚未加入该实训项目，无法选择小组");
        }
        Integer joinedGroupId = groupMemberMapper.selectGroupIdByStudent(group.getProjectId(), current.getId());
        if (joinedGroupId != null) {
            throw new CustomerException(joinedGroupId.equals(groupId) ? "你已在该小组中" : "你已加入其他小组，请先退出");
        }
        if (group.getMaxMembers() != null && group.getMaxMembers() > 0
                && countMembers(groupId) >= group.getMaxMembers()) {
            throw new CustomerException("该小组人数已满，请选择其他小组");
        }
        groupMemberMapper.physicalDeleteByGroupAndStudent(groupId, current.getId());
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setStudentId(current.getId());
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);
        notifyGroupInvite(group, List.of(current.getId()));
    }

    /**
     * 学生退出自己的小组，组长需先由教师更换。
     */
    @Transactional
    public void studentLeave(Integer groupId) {
        User current = CurrentUserContext.get();
        if (current == null || !RoleEnum.STUDENT.name().equals(current.getRole())) {
            throw new CustomerException("仅学生可退出小组");
        }
        StudentGroup group = studentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomerException("分组不存在");
        }
        if (current.getId().equals(group.getLeaderId())) {
            throw new CustomerException("你是组长，请先联系指导教师更换组长后再退出");
        }
        groupMemberMapper.physicalDeleteByGroupAndStudent(groupId, current.getId());
    }

    private long countMembers(Integer groupId) {
        return groupMemberMapper.selectCount(Wrappers.lambdaQuery(GroupMember.class)
                .eq(GroupMember::getGroupId, groupId));
    }

    private String displayName(User user) {
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername();
    }

    /**
     * 向被加入分组的学生发送分组通知。
     */
    private void notifyGroupInvite(StudentGroup group, List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        Project project = projectMapper.selectById(group.getProjectId());
        String projectName = project != null ? project.getName() : "";
        String groupName = (group.getName() != null && !group.getName().isBlank())
                ? group.getName() : group.getGroupNo();
        String content = "你已被加入实训项目「" + projectName + "」的分组「" + groupName + "」";
        String url = "/project/" + group.getProjectId() + "/board";
        for (Integer studentId : studentIds) {
            notificationService.send(studentId, "GROUP_INVITE", "分组通知", content, url);
        }
    }

    private Project getWritableProject(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new CustomerException("项目不存在");
        }
        if (ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结，禁止修改项目成员与分组");
        }
        return project;
    }
}