package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("task_attachment")
public class TaskAttachment extends BaseEntity {

    private Integer taskId;

    private Integer fileId;

    @TableField(exist = false)
    private String fileName;

    @TableField(exist = false)
    private String filePath;

    @TableField(exist = false)
    private Long fileSize;

    @TableField(exist = false)
    private String mimeType;

    @TableField(exist = false)
    private Integer uploaderId;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Integer uploaderId) {
        this.uploaderId = uploaderId;
    }
}