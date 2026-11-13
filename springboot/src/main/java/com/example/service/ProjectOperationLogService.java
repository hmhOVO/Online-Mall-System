package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.entity.OperationLog;
import com.example.entity.ProjectMember;
import com.example.mapper.OperationLogMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ProjectMemberMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjectOperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectMapper projectMapper;
    private final NotificationWebSocketService notificationWebSocketService;

    public ProjectOperationLogService(OperationLogMapper operationLogMapper,
                                      ProjectMemberMapper projectMemberMapper,
                                      ProjectMapper projectMapper,
                                      NotificationWebSocketService notificationWebSocketService) {
        this.operationLogMapper = operationLogMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.projectMapper = projectMapper;
        this.notificationWebSocketService = notificationWebSocketService;
    }

    public void saveLog(Integer projectId, Integer operatorId, String username, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(operatorId);
        log.setUsername(username);
        log.setProjectId(projectId);
        log.setOperationType(action);
        log.setMethodName("ProjectOperationLog");
        log.setParams(detail);
        log.setStatus(1);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);

        if (projectId != null) {
            LambdaQueryWrapper<ProjectMember> memberWrapper = Wrappers.lambdaQuery();
            memberWrapper.eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getDeleted, 0);
            List<ProjectMember> members = projectMemberMapper.selectList(memberWrapper);
            Set<Integer> userIds = new HashSet<>();
            for (ProjectMember member : members) {
                userIds.add(member.getUserId());
            }
            var project = projectMapper.selectById(projectId);
            if (project != null && project.getOwnerId() != null) {
                userIds.add(project.getOwnerId());
            }
            notificationWebSocketService.sendLogUpdate(projectId, userIds);
        }
    }

    public List<OperationLog> listLogs(Integer projectId) {
        return operationLogMapper.selectByProjectId(projectId);
    }
}
