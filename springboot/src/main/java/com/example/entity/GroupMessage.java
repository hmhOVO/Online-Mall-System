package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("group_message")
public class GroupMessage extends BaseEntity {

    private Integer groupId;

    private Integer senderId;

    private String content;

    @TableField(exist = false)
    private String senderName;

    @TableField(exist = false)
    private String senderUsername;

    @TableField(exist = false)
    private Boolean isLeader;

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public Boolean getIsLeader() {
        return isLeader;
    }

    public void setIsLeader(Boolean isLeader) {
        this.isLeader = isLeader;
    }
}
