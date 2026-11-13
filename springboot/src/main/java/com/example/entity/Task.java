package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@TableName("task_info")
public class Task extends BaseEntity {

    private Integer projectId;

    private Integer parentTaskId;

    private Integer taskLevel;

    private String taskType;

    private String difficulty;

    private String title;

    private String description;

    private String priority;

    private String status;

    private Integer progress;

    private Double plannedHours;

    private Double actualHours;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer assigneeId;

    private Integer groupId;

    private Integer creatorId;

    private Integer reviewerId;

    private String reviewComment;

    private Double score;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @TableField(exist = false)
    private String assigneeName;

    @TableField(exist = false)
    private String creatorName;

    @TableField(exist = false)
    private String reviewerName;

    @TableField(exist = false)
    private List<Task> children;

    @TableField(exist = false)
    private List<Integer> dependencyIds;

    @TableField(exist = false)
    private List<Task> dependencies;

    @TableField(exist = false)
    private List<TaskComment> comments;

    @TableField(exist = false)
    private Boolean overdue;

    @TableField(exist = false)
    private String projectName;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Integer parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public Integer getTaskLevel() {
        return taskLevel;
    }

    public void setTaskLevel(Integer taskLevel) {
        this.taskLevel = taskLevel;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Double getPlannedHours() {
        return plannedHours;
    }

    public void setPlannedHours(Double plannedHours) {
        this.plannedHours = plannedHours;
    }

    public Double getActualHours() {
        return actualHours;
    }

    public void setActualHours(Double actualHours) {
        this.actualHours = actualHours;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public List<Task> getChildren() {
        return children;
    }

    public void setChildren(List<Task> children) {
        this.children = children;
    }

    public List<Integer> getDependencyIds() {
        return dependencyIds;
    }

    public void setDependencyIds(List<Integer> dependencyIds) {
        this.dependencyIds = dependencyIds;
    }

    public List<Task> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Task> dependencies) {
        this.dependencies = dependencies;
    }

    public List<TaskComment> getComments() {
        return comments;
    }

    public void setComments(List<TaskComment> comments) {
        this.comments = comments;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
