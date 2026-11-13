package com.example.dto;

import com.example.entity.Task;

import java.util.List;

public class AiSplitConfirmRequest {

    private Integer projectId;
    private List<Task> tasks;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
