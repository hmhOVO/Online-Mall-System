package com.example.service;

import com.example.entity.Task;
import com.example.entity.TaskDependency;
import com.example.exception.CustomerException;
import com.example.mapper.TaskDependencyMapper;
import com.example.mapper.TaskMapper;
import com.example.utils.CycleDetectionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TaskDependencyService {

    private final TaskDependencyMapper taskDependencyMapper;
    private final TaskMapper taskMapper;

    public TaskDependencyService(TaskDependencyMapper taskDependencyMapper, TaskMapper taskMapper) {
        this.taskDependencyMapper = taskDependencyMapper;
        this.taskMapper = taskMapper;
    }

    public boolean checkDependencyCycle(Integer taskId, List<Integer> dependencyIds) {
        if (dependencyIds == null || dependencyIds.isEmpty()) {
            return false;
        }
        Set<Integer> allTaskIds = new HashSet<>(dependencyIds);
        allTaskIds.add(taskId);
        List<TaskDependency> existing = taskDependencyMapper.selectByTaskIdSet(allTaskIds);

        Map<Integer, List<Integer>> edges = new HashMap<>();
        for (Integer id : allTaskIds) {
            edges.putIfAbsent(id, new ArrayList<>());
        }
        for (TaskDependency dep : existing) {
            edges.computeIfAbsent(dep.getTaskId(), k -> new ArrayList<>()).add(dep.getDependsOnTaskId());
        }
        for (Integer depId : dependencyIds) {
            edges.computeIfAbsent(taskId, k -> new ArrayList<>()).add(depId);
        }
        return CycleDetectionUtil.hasCycle(allTaskIds, edges);
    }

    @Transactional
    public void saveDependencies(Integer taskId, Integer projectId, List<Integer> dependencyIds) {
        taskDependencyMapper.physicalDeleteByTaskId(taskId);
        if (dependencyIds == null || dependencyIds.isEmpty()) {
            return;
        }
        if (checkDependencyCycle(taskId, dependencyIds)) {
            throw new CustomerException("存在循环依赖");
        }
        for (Integer depId : dependencyIds) {
            Task depTask = getTask(depId);
            if (!depTask.getProjectId().equals(projectId)) {
                throw new CustomerException("依赖任务不在同一项目");
            }
            TaskDependency dep = new TaskDependency();
            dep.setTaskId(taskId);
            dep.setDependsOnTaskId(depId);
            taskDependencyMapper.insert(dep);
        }
    }

    public List<TaskDependency> getTaskDependencies(Integer taskId) {
        return taskDependencyMapper.selectByTaskId(taskId);
    }

    public List<Integer> getDependsOnIds(Integer taskId) {
        return taskDependencyMapper.selectDependsOnIds(taskId);
    }

    public List<TaskDependency> selectDependsOnByTaskIds(java.util.Collection<Integer> taskIds) {
        return taskDependencyMapper.selectDependsOnByTaskIds(taskIds);
    }

    @Transactional
    public void deleteByTaskId(Integer taskId) {
        taskDependencyMapper.physicalDeleteByTaskId(taskId);
    }

    @Transactional
    public void deleteByTaskIdOrDependsOn(Integer taskId) {
        taskDependencyMapper.physicalDeleteByTaskIdOrDependsOn(taskId);
    }

    private Task getTask(Integer id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new CustomerException("任务不存在");
        }
        return task;
    }
}