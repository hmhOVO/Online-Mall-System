package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

@TableName("task_comment")
public class TaskComment extends BaseEntity {

    private Integer taskId;

    private Integer userId;

    private Integer parentCommentId;

    private String content;

    private Boolean isResolved;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private List<TaskComment> replies;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public List<TaskComment> getReplies() {
        return replies;
    }

    public void setReplies(List<TaskComment> replies) {
        this.replies = replies;
    }
}
