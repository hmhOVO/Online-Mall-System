package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.CurrentUserContext;
import com.example.common.ProjectConstants;
import com.example.common.ProjectRoleLevel;
import com.example.common.TaskConstants;
import com.example.dto.CopyProjectRequest;
import com.example.entity.GroupMember;
import com.example.entity.Project;
import com.example.entity.ProjectMember;
import com.example.entity.StudentGroup;
import com.example.entity.Task;
import com.example.entity.TaskDependency;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.GroupMemberMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ProjectMemberMapper;
import com.example.mapper.StudentGroupMapper;
import com.example.mapper.TaskDependencyMapper;
import com.example.mapper.TaskMapper;
import com.example.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final StudentGroupMapper studentGroupMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final TaskDependencyMapper taskDependencyMapper;
    private final NotificationService notificationService;
    private final ProjectOperationLogService projectOperationLogService;

    public ProjectService(ProjectMapper projectMapper, ProjectMemberMapper projectMemberMapper,
                          GroupMemberMapper groupMemberMapper, StudentGroupMapper studentGroupMapper,
                          UserMapper userMapper, TaskMapper taskMapper, TaskDependencyMapper taskDependencyMapper,
                          NotificationService notificationService,
                          ProjectOperationLogService projectOperationLogService) {
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.studentGroupMapper = studentGroupMapper;
        this.userMapper = userMapper;
        this.taskMapper = taskMapper;
        this.taskDependencyMapper = taskDependencyMapper;
        this.notificationService = notificationService;
        this.projectOperationLogService = projectOperationLogService;
    }

    @Transactional
    public void createProject(Project project) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.TEACHER.name().equals(current.getRole())) {
            throw new CustomerException("仅指导教师可创建项目");
        }
        if (project.getTeacherId() == null) {
            project.setTeacherId(current.getId());
        }
        validateProjectName(project.getName(), null, project.getTeacherId(), project.getClassId());
        validateProjectDates(project);
        project.setStatus(ProjectConstants.PROJECT_STATUS_NOT_STARTED);
        project.setCreatorId(current.getId());
        project.setOwnerId(current.getId());
        if (project.getProjectType() == null || project.getProjectType().isBlank()) {
            project.setProjectType("COURSE_TRAINING");
        }
        projectMapper.insert(project);

        enrollClassStudents(project);

        saveLog(project.getId(), "创建项目", "创建人：" + current.getUsername());
    }

    /**
     * 创建项目时若已绑定班级，自动把该班在籍学生加入项目，避免教师逐个导入。
     */
    private void enrollClassStudents(Project project) {
        if (project.getClassId() == null) {
            return;
        }
        LambdaQueryWrapper<User> studentWrapper = Wrappers.lambdaQuery();
        studentWrapper.eq(User::getRole, RoleEnum.STUDENT.name())
                .eq(User::getClassId, project.getClassId())
                .eq(User::getStatus, "ENABLED");
        List<User> students = userMapper.selectList(studentWrapper);
        for (User student : students) {
            LambdaQueryWrapper<ProjectMember> activeWrapper = Wrappers.lambdaQuery();
            activeWrapper.eq(ProjectMember::getProjectId, project.getId())
                    .eq(ProjectMember::getUserId, student.getId());
            if (projectMemberMapper.selectCount(activeWrapper) > 0) {
                continue;
            }
            ProjectMember deletedMember = projectMemberMapper.selectDeletedMember(project.getId(), student.getId());
            if (deletedMember != null) {
                LocalDateTime now = LocalDateTime.now();
                projectMemberMapper.restoreMember(deletedMember.getId(), now, now);
            } else {
                ProjectMember member = new ProjectMember();
                member.setProjectId(project.getId());
                member.setUserId(student.getId());
                member.setJoinedAt(LocalDateTime.now());
                projectMemberMapper.insert(member);
            }
        }
    }

    @Transactional
    public Integer copyProject(Integer sourceId, CopyProjectRequest request) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!RoleEnum.TEACHER.name().equals(current.getRole())) {
            throw new CustomerException("仅指导教师可创建项目");
        }
        Project source = getAndCheckProject(sourceId);
        boolean isSourceTeacher = Objects.equals(source.getTeacherId(), current.getId())
                || Objects.equals(source.getOwnerId(), current.getId());
        if (!isSourceTeacher) {
            throw new CustomerException("仅项目指导教师可复制项目");
        }

        Project project = new Project();
        project.setName(request.getName() != null && !request.getName().isBlank()
                ? request.getName().trim() : source.getName() + "（副本）");
        project.setDescription(source.getDescription());
        project.setTags(source.getTags());
        project.setProjectType(source.getProjectType());
        project.setCourseName(source.getCourseName());
        project.setSchoolId(request.getSchoolId() != null ? request.getSchoolId() : source.getSchoolId());
        project.setMajorId(request.getMajorId() != null ? request.getMajorId() : source.getMajorId());
        project.setClassId(request.getClassId() != null ? request.getClassId() : source.getClassId());
        project.setCourseId(source.getCourseId());
        project.setTeacherId(request.getTeacherId() != null ? request.getTeacherId() : source.getTeacherId());
        project.setStartDate(request.getStartDate() != null ? request.getStartDate() : source.getStartDate());
        project.setEndDate(request.getEndDate() != null ? request.getEndDate() : source.getEndDate());
        validateProjectName(project.getName(), null, project.getTeacherId(), project.getClassId());
        validateProjectDates(project);
        project.setStatus(ProjectConstants.PROJECT_STATUS_NOT_STARTED);
        project.setCreatorId(current.getId());
        project.setOwnerId(current.getId());
        projectMapper.insert(project);

        if (Boolean.TRUE.equals(request.getCopyMembers())) {
            copyProjectMembers(sourceId, project.getId());
        }
        if (Boolean.TRUE.equals(request.getCopyTasks())) {
            copyProjectTasks(sourceId, project.getId(), current.getId());
        }
        if (Boolean.TRUE.equals(request.getCopyGroups())) {
            copyProjectGroups(sourceId, project.getId());
        }

        saveLog(project.getId(), "复制项目", "从项目「" + source.getName() + "」复制");
        return project.getId();
    }

    public void updateProject(Integer id, Project project) {
        Project existing = getAndCheckProject(id);
        checkWritePermission(existing);
        if (ProjectConstants.PROJECT_STATUS_COMPLETED.equals(existing.getStatus())) {
            throw new CustomerException("已完成项目不可编辑");
        }
        validateProjectName(project.getName(), id, project.getTeacherId(), project.getClassId());
        validateProjectDates(project);
        LambdaUpdateWrapper<Project> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Project::getId, id)
                .set(Project::getName, project.getName())
                .set(Project::getDescription, project.getDescription())
                .set(Project::getTags, project.getTags())
                .set(Project::getProjectType, project.getProjectType())
                .set(Project::getCourseName, project.getCourseName())
                .set(Project::getTeacherId, project.getTeacherId())
                .set(Project::getSchoolId, project.getSchoolId())
                .set(Project::getMajorId, project.getMajorId())
                .set(Project::getClassId, project.getClassId())
                .set(Project::getCourseId, project.getCourseId())
                .set(Project::getStartDate, project.getStartDate())
                .set(Project::getEndDate, project.getEndDate());
        projectMapper.update(null, wrapper);
        saveLog(id, "编辑项目", "更新了项目基本信息");
    }

    @Transactional
    public void deleteProject(Integer id) {
        Project project = getAndCheckProject(id);
        checkOwnerPermission(project);
        if (!ProjectConstants.PROJECT_STATUS_NOT_STARTED.equals(project.getStatus())) {
            throw new CustomerException("仅未开始的项目可删除，进行中或已完成的项目请使用归档");
        }

        // 软删除项目
        projectMapper.deleteById(id);

        // 软删除项目成员
        LambdaUpdateWrapper<ProjectMember> memberWrapper = Wrappers.lambdaUpdate();
        memberWrapper.eq(ProjectMember::getProjectId, id);
        projectMemberMapper.delete(memberWrapper);

        // 软删除项目下的任务
        LambdaUpdateWrapper<Task> taskWrapper = Wrappers.lambdaUpdate();
        taskWrapper.eq(Task::getProjectId, id);
        taskMapper.delete(taskWrapper);

        saveLog(id, "删除项目", "项目「" + project.getName() + "」被删除");
    }

    @Transactional
    public void archiveProject(Integer id, boolean archived) {
        Project project = getAndCheckProject(id);
        checkOwnerPermission(project);
        Project update = new Project();
        update.setId(id);
        update.setArchived(archived ? 1 : 0);
        projectMapper.updateById(update);
        saveLog(id, archived ? "归档项目" : "取消归档",
                "项目「" + project.getName() + "」" + (archived ? "已归档" : "已取消归档"));
    }

    public Page<Project> listDeletedProjects(Integer pageNum, Integer pageSize, String name, String status) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }

        List<Integer> projectIds = null;
        boolean canViewAll = RoleEnum.ADMIN.name().equals(current.getRole());
        if (!canViewAll) {
            projectIds = projectMapper.selectOwnDeletedProjectIds(current.getId());
            if (projectIds == null || projectIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
        }

        int offset = (pageNum - 1) * pageSize;
        List<Project> projects = projectMapper.selectDeletedProjectsWithTaskStats(name, status, projectIds, offset, pageSize);
        long total = projectMapper.countDeletedProjectsWithTaskStats(name, status, projectIds);
        fillUserNames(projects);

        Page<Project> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(projects);
        return page;
    }

    @Transactional
    public void restoreProject(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        Project project = projectMapper.selectDeletedById(id);
        if (project == null) {
            throw new CustomerException("项目不存在或未被删除");
        }
        boolean canRestore = RoleEnum.ADMIN.name().equals(current.getRole())
                || Objects.equals(project.getOwnerId(), current.getId())
                || Objects.equals(project.getCreatorId(), current.getId())
                || Objects.equals(project.getTeacherId(), current.getId());
        if (!canRestore) {
            throw new CustomerException("仅管理员、项目负责人、创建人或指导教师可恢复项目");
        }
        int rows = projectMapper.restoreProjectById(id);
        if (rows == 0) {
            throw new CustomerException("项目恢复失败，项目可能不存在或未被删除");
        }

        // 恢复项目成员
        projectMemberMapper.restoreMembersByProjectId(id);

        // 恢复项目下的任务
        LambdaUpdateWrapper<Task> taskWrapper = Wrappers.lambdaUpdate();
        taskWrapper.eq(Task::getProjectId, id).set(Task::getDeleted, 0);
        taskMapper.update(null, taskWrapper);

        saveLog(id, "恢复项目", "项目「" + project.getName() + "」已从回收站恢复");
    }

    @Transactional
    public void purgeProject(Integer id) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        Project project = projectMapper.selectDeletedById(id);
        if (project == null) {
            throw new CustomerException("项目不存在或不在回收站中");
        }
        boolean canPurge = RoleEnum.ADMIN.name().equals(current.getRole())
                || Objects.equals(project.getOwnerId(), current.getId())
                || Objects.equals(project.getCreatorId(), current.getId())
                || Objects.equals(project.getTeacherId(), current.getId());
        if (!canPurge) {
            throw new CustomerException("仅管理员、项目负责人、创建人或指导教师可彻底删除项目");
        }
        purgeProjectData(id);
    }

    @Transactional
    public int purgeAllDeletedProjects() {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        List<Integer> ids = RoleEnum.ADMIN.name().equals(current.getRole())
                ? projectMapper.selectAllDeletedProjectIds()
                : projectMapper.selectOwnDeletedProjectIds(current.getId());
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        ids.forEach(this::purgeProjectData);
        return ids.size();
    }

    private void purgeProjectData(Integer id) {
        projectMapper.deleteTaskDependenciesByProjectId(id);
        projectMapper.deleteTaskCommentsByProjectId(id);
        projectMapper.deleteTaskAttachmentsByProjectId(id);
        projectMapper.deleteTaskChangeLogsByProjectId(id);
        projectMapper.deleteGroupMembersByProjectId(id);
        projectMapper.deleteStudentGroupsByProjectId(id);
        projectMapper.deleteMembersByProjectId(id);
        projectMapper.deleteFilesByProjectId(id);
        projectMapper.deleteTasksByProjectId(id);
        projectMapper.hardDeleteById(id);
    }

    public Page<Project> listProjects(Integer pageNum, Integer pageSize, String name, String status,
                                      LocalDate startDateBegin, LocalDate startDateEnd, Integer archived, Integer courseId) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }

        List<Integer> projectIds = null;
        boolean canViewAll = RoleEnum.ADMIN.name().equals(current.getRole());
        if (!canViewAll) {
            Set<Integer> projectIdSet = new java.util.HashSet<>();
            LambdaQueryWrapper<ProjectMember> memberWrapper = Wrappers.lambdaQuery();
            memberWrapper.eq(ProjectMember::getUserId, current.getId());
            projectMemberMapper.selectList(memberWrapper).forEach(m -> projectIdSet.add(m.getProjectId()));

            // 非管理员：指导教师或项目负责人身份的项目也并入可见集合
            LambdaQueryWrapper<Project> projectWrapper = Wrappers.lambdaQuery();
            projectWrapper.eq(Project::getDeleted, 0)
                    .and(w -> w.eq(Project::getOwnerId, current.getId())
                            .or().eq(Project::getTeacherId, current.getId()));
            projectMapper.selectList(projectWrapper).forEach(p -> projectIdSet.add(p.getId()));

            if (projectIdSet.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            projectIds = projectIdSet.stream().toList();
        }

        int offset = (pageNum - 1) * pageSize;
        List<Project> projects = projectMapper.selectProjectsWithTaskStats(
                name, status, startDateBegin, startDateEnd, courseId, projectIds, archived, offset, pageSize);
        long total = projectMapper.countProjectsWithTaskStats(name, status, startDateBegin, startDateEnd, courseId, projectIds, archived);

        fillUserNames(projects);

        Page<Project> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(projects);
        return page;
    }

    public Project getProjectDetail(Integer id) {
        Project project = getAndCheckProject(id);
        checkReadPermission(project);
        fillUserNames(Arrays.asList(project));
        project.setMembers(projectMemberMapper.selectMembersByProjectId(id));
        return project;
    }

    public List<Map<String, Object>> getProjectScores(Integer id) {
        Project project = getAndCheckProject(id);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        boolean allowed = RoleEnum.ADMIN.name().equals(current.getRole())
                || Objects.equals(project.getTeacherId(), current.getId());
        if (!allowed) {
            throw new CustomerException("仅指导教师可查看成绩汇总");
        }
        return taskMapper.selectScoreSummary(id);
    }

    public List<Map<String, Object>> getGroupScores(Integer id) {
        Project project = getAndCheckProject(id);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        boolean allowed = RoleEnum.ADMIN.name().equals(current.getRole())
                || Objects.equals(project.getTeacherId(), current.getId());
        if (!allowed) {
            throw new CustomerException("仅指导教师可查看小组统计");
        }
        return taskMapper.selectGroupScoreSummary(id);
    }

    @Transactional
    public void addMember(Integer projectId, Integer userId) {
        Project project = getAndCheckProject(projectId);
        checkWritePermission(project);
        checkProjectWritable(project);
        User targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new CustomerException("目标用户不存在");
        }
        if (!RoleEnum.STUDENT.name().equals(targetUser.getRole())) {
            throw new CustomerException("只能将学生加入项目/小组");
        }
        LambdaQueryWrapper<ProjectMember> activeWrapper = Wrappers.lambdaQuery();
        activeWrapper.eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId);
        if (projectMemberMapper.selectCount(activeWrapper) > 0) {
            throw new CustomerException("该用户已是项目成员");
        }

        ProjectMember deletedMember = projectMemberMapper.selectDeletedMember(projectId, userId);
        if (deletedMember != null) {
            LocalDateTime now = LocalDateTime.now();
            projectMemberMapper.restoreMember(deletedMember.getId(), now, now);
        } else {
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(userId);
            member.setJoinedAt(LocalDateTime.now());
            projectMemberMapper.insert(member);
        }
        saveLog(projectId, "添加成员", "添加成员：" + targetUser.getUsername());

        notificationService.send(userId, "MEMBER", "项目成员变动",
                String.format("你被加入项目「%s」", project.getName()),
                "/project/" + projectId);
    }

    @Transactional
    public void removeMember(Integer projectId, Integer userId) {
        Project project = getAndCheckProject(projectId);
        checkWritePermission(project);
        checkProjectWritable(project);
        Integer leaderId = groupMemberMapper.selectLeaderIdByStudent(projectId, userId);
        if (leaderId != null && leaderId.equals(userId)) {
            throw new CustomerException("该成员是小组组长，请先在分组管理中更换组长");
        }
        LambdaUpdateWrapper<ProjectMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, userId);
        projectMemberMapper.delete(wrapper);
        User targetUser = userMapper.selectById(userId);
        saveLog(projectId, "移除成员", "移除成员：" + (targetUser == null ? userId : targetUser.getUsername()));

        notificationService.send(userId, "MEMBER", "项目成员变动",
                String.format("你已被移出项目「%s」", project.getName()), null);
    }

    @Transactional
    public void updateStatus(Integer projectId, String newStatus) {
        Project project = getAndCheckProject(projectId);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!canManageProject(project, current)) {
            throw new CustomerException("无权限变更项目状态");
        }
        if (ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结，请先重启项目");
        }
        validateStatusTransition(project.getStatus(), newStatus);
        LambdaUpdateWrapper<Project> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Project::getId, projectId).set(Project::getStatus, newStatus);
        projectMapper.update(null, wrapper);
        saveLog(projectId, "变更状态", "状态由 " + project.getStatus() + " 变为 " + newStatus);

        if (ProjectConstants.PROJECT_STATUS_COMPLETED.equals(newStatus)) {
            notifyProjectCompleted(project);
        }
    }

    public long countUnfinishedTasks(Integer projectId) {
        LambdaQueryWrapper<Task> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Task::getProjectId, projectId)
                .ne(Task::getStatus, "DONE")
                .eq(Task::getDeleted, 0);
        return taskMapper.selectCount(wrapper);
    }

    @Transactional
    public void completeProject(Integer projectId) {
        Project project = getAndCheckProject(projectId);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!canManageProject(project, current)) {
            throw new CustomerException("无权限变更项目状态");
        }
        if (ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结");
        }

        // 批量将未完成任务标记为已完成
        LambdaUpdateWrapper<Task> taskWrapper = Wrappers.lambdaUpdate();
        taskWrapper.eq(Task::getProjectId, projectId)
                .ne(Task::getStatus, "DONE")
                .eq(Task::getDeleted, 0)
                .set(Task::getStatus, "DONE")
                .set(Task::getUpdatedAt, LocalDateTime.now());
        taskMapper.update(null, taskWrapper);

        // 项目状态变更为已完成
        LambdaUpdateWrapper<Project> projectWrapper = Wrappers.lambdaUpdate();
        projectWrapper.eq(Project::getId, projectId)
                .set(Project::getStatus, ProjectConstants.PROJECT_STATUS_COMPLETED);
        projectMapper.update(null, projectWrapper);

        saveLog(projectId, "项目结项", "项目完成，所有未完成任务已标记为已完成");
        notifyProjectCompleted(project);
    }

    public void restartProject(Integer projectId) {
        Project project = getAndCheckProject(projectId);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (!canManageProject(project, current)) {
            throw new CustomerException("无权限重启项目");
        }
        if (!ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("只有已完成项目可以重启");
        }
        LambdaUpdateWrapper<Project> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Project::getId, projectId).set(Project::getStatus, ProjectConstants.PROJECT_STATUS_IN_PROGRESS);
        projectMapper.update(null, wrapper);
        saveLog(projectId, "重启项目", "项目重启为进行中");
    }

    /**
     * 变更指导教师（项目归属）：仅在教师之间转移，owner_id 与 teacher_id 同步更新，永不移交给学生。
     */
    @Transactional
    public void transferProject(Integer projectId, Integer newTeacherId) {
        Project project = getAndCheckProject(projectId);
        checkProjectWritable(project);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        boolean isAdmin = RoleEnum.ADMIN.name().equals(current.getRole());
        boolean isCurrentTeacher = Objects.equals(project.getTeacherId(), current.getId());
        if (!isAdmin && !isCurrentTeacher) {
            throw new CustomerException("仅当前指导教师或管理员可变更为指导教师");
        }
        if (newTeacherId == null) {
            throw new CustomerException("新指导教师不能为空");
        }
        if (Objects.equals(newTeacherId, project.getOwnerId()) || Objects.equals(newTeacherId, project.getTeacherId())) {
            throw new CustomerException("新指导教师不能与当前指导教师相同");
        }
        User newTeacher = userMapper.selectById(newTeacherId);
        if (newTeacher == null) {
            throw new CustomerException("新指导教师不存在");
        }
        if (!RoleEnum.TEACHER.name().equals(newTeacher.getRole())) {
            throw new CustomerException("新指导教师必须是教师");
        }
        LambdaUpdateWrapper<Project> projectWrapper = Wrappers.lambdaUpdate();
        projectWrapper.eq(Project::getId, projectId)
                .set(Project::getOwnerId, newTeacherId)
                .set(Project::getTeacherId, newTeacherId);
        projectMapper.update(null, projectWrapper);
        saveLog(projectId, "变更指导教师", "指导教师变更为：" + newTeacher.getUsername());
    }

    /**
     * 指定/更换组长：只调整该学生所在小组的 leader_id，不动项目归属（owner_id/teacher_id）。
     */
    @Transactional
    public void assignGroupLeader(Integer projectId, Integer studentId) {
        Project project = getAndCheckProject(projectId);
        checkProjectWritable(project);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        boolean isAdmin = RoleEnum.ADMIN.name().equals(current.getRole());
        boolean isCurrentTeacher = Objects.equals(project.getTeacherId(), current.getId());
        if (!isAdmin && !isCurrentTeacher) {
            throw new CustomerException("仅指导教师或管理员可指定组长");
        }
        if (studentId == null) {
            throw new CustomerException("目标学生不能为空");
        }
        User student = userMapper.selectById(studentId);
        if (student == null) {
            throw new CustomerException("目标学生不存在");
        }
        if (!RoleEnum.STUDENT.name().equals(student.getRole())) {
            throw new CustomerException("组长必须由学生担任");
        }
        LambdaQueryWrapper<ProjectMember> memberWrapper = Wrappers.lambdaQuery();
        memberWrapper.eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, studentId);
        if (projectMemberMapper.selectCount(memberWrapper) == 0) {
            throw new CustomerException("该学生不是项目成员，请先加入项目");
        }

        Integer groupId = groupMemberMapper.selectGroupIdByStudent(projectId, studentId);
        if (groupId == null) {
            throw new CustomerException("该学生尚未加入任何小组，请先在分组管理中调整分组");
        }

        LambdaUpdateWrapper<StudentGroup> leaderWrapper = Wrappers.lambdaUpdate();
        leaderWrapper.eq(StudentGroup::getId, groupId)
                .set(StudentGroup::getLeaderId, studentId);
        studentGroupMapper.update(null, leaderWrapper);

        saveLog(projectId, "指定组长", "小组组长变更为：" + student.getUsername());
    }

    public List<com.example.entity.OperationLog> listLogs(Integer projectId) {
        Project project = getAndCheckProject(projectId);
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return projectOperationLogService.listLogs(projectId);
        }
        boolean isOwner = Objects.equals(project.getOwnerId(), current.getId());
        boolean isTeacher = Objects.equals(project.getTeacherId(), current.getId());
        List<ProjectMember> projectMemberList = projectMemberMapper.selectByProjectId(projectId);
        boolean isMember = projectMemberList.stream().anyMatch(m -> m.getUserId().equals(current.getId()));
        if (!isMember && !isOwner && !isTeacher) {
            throw new CustomerException("无权限查看项目日志");
        }
        return projectOperationLogService.listLogs(projectId);
    }

    private void copyProjectMembers(Integer sourceId, Integer targetId) {
        List<ProjectMember> members = projectMemberMapper.selectList(
                Wrappers.lambdaQuery(ProjectMember.class).eq(ProjectMember::getProjectId, sourceId));
        for (ProjectMember source : members) {
            ProjectMember member = new ProjectMember();
            member.setProjectId(targetId);
            member.setUserId(source.getUserId());
            member.setJoinedAt(LocalDateTime.now());
            projectMemberMapper.insert(member);
        }
    }

    private void copyProjectTasks(Integer sourceId, Integer targetId, Integer userId) {
        List<Task> tasks = taskMapper.selectList(
                Wrappers.lambdaQuery(Task.class).eq(Task::getProjectId, sourceId).orderByAsc(Task::getId));
        Map<Integer, Integer> taskIdMap = new HashMap<>();
        for (Task source : tasks) {
            Task task = new Task();
            task.setProjectId(targetId);
            task.setTaskLevel(source.getTaskLevel());
            task.setTaskType(source.getTaskType());
            task.setDifficulty(source.getDifficulty());
            task.setTitle(source.getTitle());
            task.setDescription(source.getDescription());
            task.setPriority(source.getPriority());
            task.setPlannedHours(source.getPlannedHours());
            task.setStartDate(source.getStartDate());
            task.setEndDate(source.getEndDate());
            task.setAssigneeId(source.getAssigneeId());
            task.setCreatorId(userId);
            task.setReviewerId(source.getReviewerId());
            task.setStatus(TaskConstants.STATUS_TODO);
            task.setProgress(0);
            taskMapper.insert(task);
            taskIdMap.put(source.getId(), task.getId());
        }
        // 子任务的父任务 ID 需要等新任务落库后再回填
        for (Task source : tasks) {
            if (source.getParentTaskId() == null) {
                continue;
            }
            Integer newParentId = taskIdMap.get(source.getParentTaskId());
            if (newParentId == null) {
                continue;
            }
            taskMapper.update(null, Wrappers.lambdaUpdate(Task.class)
                    .eq(Task::getId, taskIdMap.get(source.getId()))
                    .set(Task::getParentTaskId, newParentId));
        }
        if (taskIdMap.isEmpty()) {
            return;
        }
        List<TaskDependency> dependencies = taskDependencyMapper.selectList(
                Wrappers.lambdaQuery(TaskDependency.class).in(TaskDependency::getTaskId, taskIdMap.keySet()));
        for (TaskDependency source : dependencies) {
            Integer newTaskId = taskIdMap.get(source.getTaskId());
            Integer newDependsOnId = taskIdMap.get(source.getDependsOnTaskId());
            if (newTaskId == null || newDependsOnId == null) {
                continue;
            }
            TaskDependency dependency = new TaskDependency();
            dependency.setTaskId(newTaskId);
            dependency.setDependsOnTaskId(newDependsOnId);
            taskDependencyMapper.insert(dependency);
        }
    }

    private void copyProjectGroups(Integer sourceId, Integer targetId) {
        List<StudentGroup> groups = studentGroupMapper.selectList(
                Wrappers.lambdaQuery(StudentGroup.class).eq(StudentGroup::getProjectId, sourceId));
        for (StudentGroup source : groups) {
            StudentGroup group = new StudentGroup();
            group.setProjectId(targetId);
            group.setName(source.getName());
            group.setGroupNo(source.getGroupNo());
            group.setLeaderId(source.getLeaderId());
            group.setDescription(source.getDescription());
            studentGroupMapper.insert(group);

            List<GroupMember> members = groupMemberMapper.selectList(
                    Wrappers.lambdaQuery(GroupMember.class).eq(GroupMember::getGroupId, source.getId()));
            for (GroupMember sourceMember : members) {
                GroupMember member = new GroupMember();
                member.setGroupId(group.getId());
                member.setStudentId(sourceMember.getStudentId());
                member.setJoinedAt(LocalDateTime.now());
                groupMemberMapper.insert(member);
            }
        }
    }

    private Project getAndCheckProject(Integer id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new CustomerException("项目不存在");
        }
        return project;
    }

    private void checkOwnerPermission(Project project) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        boolean isTeacher = Objects.equals(project.getTeacherId(), current.getId());
        if (!isTeacher) {
            throw new CustomerException("仅指导教师可执行此操作");
        }
    }

    private void checkWritePermission(Project project) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        ProjectRoleLevel level = getUserProjectRole(project.getId(), current.getId());
        if (level != ProjectRoleLevel.OWNER) {
            throw new CustomerException("仅项目负责人可编辑项目");
        }
    }

    private void checkReadPermission(Project project) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return;
        }
        if (Objects.equals(project.getOwnerId(), current.getId())) {
            return;
        }
        if (Objects.equals(project.getTeacherId(), current.getId())) {
            return;
        }
        LambdaQueryWrapper<ProjectMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectMember::getProjectId, project.getId()).eq(ProjectMember::getUserId, current.getId());
        if (projectMemberMapper.selectCount(wrapper) == 0) {
            throw new CustomerException("无权限查看该项目");
        }
    }

    public ProjectRoleLevel getUserProjectRole(Integer projectId, Integer userId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return ProjectRoleLevel.NONE;
        }
        // 项目级管理权只属于指导教师；组长职责下沉到小组，由小组 leader 承担
        if (Objects.equals(project.getTeacherId(), userId)) {
            return ProjectRoleLevel.OWNER;
        }
        LambdaQueryWrapper<ProjectMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId)
                .eq(ProjectMember::getDeleted, 0);
        ProjectMember member = projectMemberMapper.selectOne(wrapper);
        if (member == null) {
            return ProjectRoleLevel.NONE;
        }
        return ProjectRoleLevel.MEMBER;
    }

    private boolean canManageProject(Project project, User current) {
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return true;
        }
        ProjectRoleLevel level = getUserProjectRole(project.getId(), current.getId());
        return level == ProjectRoleLevel.OWNER;
    }

    private void checkProjectWritable(Project project) {
        if (ProjectConstants.PROJECT_STATUS_COMPLETED.equals(project.getStatus())) {
            throw new CustomerException("项目已完结，禁止写入操作");
        }
    }

    private void notifyProjectCompleted(Project project) {
        LambdaQueryWrapper<ProjectMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectMember::getProjectId, project.getId());
        List<ProjectMember> members = projectMemberMapper.selectList(wrapper);
        List<Integer> userIds = new java.util.ArrayList<>(members.stream().map(ProjectMember::getUserId).toList());
        if (project.getTeacherId() != null && !userIds.contains(project.getTeacherId())) {
            userIds.add(project.getTeacherId());
        }
        notificationService.sendBatch(userIds, "TASK_STATUS", "项目已完结",
                String.format("项目「%s」已标记为完成", project.getName()),
                "/project/" + project.getId());
    }

    /**
     * 名称唯一性限定在「同一教师 + 同一班级」范围内，不同班级可复用同一项目名称。
     */
    private void validateProjectName(String name, Integer excludeId, Integer teacherId, Integer classId) {
        if (name == null || name.trim().isEmpty()) {
            throw new CustomerException("项目名称不能为空");
        }
        LambdaQueryWrapper<Project> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Project::getName, name.trim()).eq(Project::getDeleted, 0);
        if (teacherId == null) {
            wrapper.isNull(Project::getTeacherId);
        } else {
            wrapper.eq(Project::getTeacherId, teacherId);
        }
        if (classId == null) {
            wrapper.isNull(Project::getClassId);
        } else {
            wrapper.eq(Project::getClassId, classId);
        }
        if (excludeId != null) {
            wrapper.ne(Project::getId, excludeId);
        }
        if (projectMapper.selectCount(wrapper) > 0) {
            throw new CustomerException("该班级下已存在同名项目，请修改名称或选择其他班级");
        }
    }

    private void validateProjectDates(Project project) {
        if (project.getStartDate() == null || project.getEndDate() == null) {
            return;
        }
        if (project.getEndDate().isBefore(project.getStartDate())) {
            throw new CustomerException("结束日期不能早于开始日期");
        }
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (Objects.equals(currentStatus, newStatus)) {
            return;
        }
        if (ProjectConstants.PROJECT_STATUS_NOT_STARTED.equals(currentStatus)
                && ProjectConstants.PROJECT_STATUS_IN_PROGRESS.equals(newStatus)) {
            return;
        }
        if (ProjectConstants.PROJECT_STATUS_IN_PROGRESS.equals(currentStatus)
                && (ProjectConstants.PROJECT_STATUS_DELAYED.equals(newStatus)
                || ProjectConstants.PROJECT_STATUS_COMPLETED.equals(newStatus))) {
            return;
        }
        if (ProjectConstants.PROJECT_STATUS_DELAYED.equals(currentStatus)
                && (ProjectConstants.PROJECT_STATUS_IN_PROGRESS.equals(newStatus)
                || ProjectConstants.PROJECT_STATUS_COMPLETED.equals(newStatus))) {
            return;
        }
        throw new CustomerException("非法的状态流转：" + currentStatus + " → " + newStatus);
    }

    private void fillUserNames(List<Project> projects) {
        for (Project project : projects) {
            if (project.getCreatorId() != null) {
                User user = userMapper.selectById(project.getCreatorId());
                project.setCreatorName(user == null ? "" : user.getNickname());
            }
            if (project.getOwnerId() != null) {
                User user = userMapper.selectById(project.getOwnerId());
                project.setOwnerName(user == null ? "" : user.getNickname());
            }
            if (project.getTeacherId() != null) {
                User user = userMapper.selectById(project.getTeacherId());
                project.setTeacherName(user == null ? "" : user.getNickname());
            }
        }
    }

    private void saveLog(Integer projectId, String action, String detail) {
        User current = CurrentUserContext.get();
        projectOperationLogService.saveLog(projectId,
                current == null ? null : current.getId(),
                current == null ? null : current.getUsername(),
                action, detail);
    }
}
