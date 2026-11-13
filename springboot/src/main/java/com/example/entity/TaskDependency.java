package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("task_dependency")
public class TaskDependency extends BaseEntity {

    private Integer taskId;

    private Integer dependsOnTaskId;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public void setDependsOnTaskId(Integer dependsOnTaskId) {
        this.dependsOnTaskId = dependsOnTaskId;
    }
}
