package com.example.service;

import com.example.common.CurrentUserContext;
import com.example.entity.ProjectMember;
import com.example.mapper.ProjectMemberMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final ProjectMemberMapper projectMemberMapper;

    public MemberService(ProjectMemberMapper projectMemberMapper) {
        this.projectMemberMapper = projectMemberMapper;
    }

    public List<ProjectMember> getProjectMembers(Integer projectId) {
        return projectMemberMapper.selectMembersByProjectId(projectId);
    }

    public List<ProjectMember> getMyProjectsMembers() {
        Integer userId = CurrentUserContext.getUserId();
        return projectMemberMapper.selectMembersByUserId(userId);
    }

    public List<Map<String, Object>> getMemberSummary() {
        Integer userId = CurrentUserContext.getUserId();
        List<Map<String, Object>> rawList = projectMemberMapper.selectMemberSummary(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> raw : rawList) {
            Map<String, Object> item = new HashMap<>(raw);
            // 转换项目名称字符串为列表
            String projectNamesStr = (String) raw.get("projectNames");
            if (projectNamesStr != null && !projectNamesStr.isEmpty()) {
                item.put("projectNames", Arrays.asList(projectNamesStr.split(",")));
            } else {
                item.put("projectNames", new ArrayList<>());
            }
            // 转换项目ID字符串为列表
            String projectIdsStr = (String) raw.get("projectIdsStr");
            if (projectIdsStr != null && !projectIdsStr.isEmpty()) {
                List<Integer> projectIds = Arrays.stream(projectIdsStr.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                item.put("projectIds", projectIds);
            } else {
                item.put("projectIds", new ArrayList<>());
            }
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getMemberAllTasks(Integer memberUserId) {
        Integer userId = CurrentUserContext.getUserId();
        return projectMemberMapper.selectMemberAllTasks(userId, memberUserId);
    }
}