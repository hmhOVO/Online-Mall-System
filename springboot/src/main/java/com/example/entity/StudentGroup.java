package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("student_group")
public class StudentGroup extends BaseEntity {

    private Integer projectId;

    private String name;

    private String groupNo;

    private Integer leaderId;

    private String description;

    /** 人数上限，0 表示不限 */
    private Integer maxMembers;

    /** 是否允许学生自选加入 */
    private Boolean joinEnabled;

    @TableField(exist = false)
    private Integer memberCount;

    @TableField(exist = false)
    private String leaderName;

    @TableField(exist = false)
    private Boolean joinedByMe;

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public Boolean getJoinEnabled() {
        return joinEnabled;
    }

    public void setJoinEnabled(Boolean joinEnabled) {
        this.joinEnabled = joinEnabled;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public Boolean getJoinedByMe() {
        return joinedByMe;
    }

    public void setJoinedByMe(Boolean joinedByMe) {
        this.joinedByMe = joinedByMe;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupNo() {
        return groupNo;
    }

    public void setGroupNo(String groupNo) {
        this.groupNo = groupNo;
    }

    public Integer getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Integer leaderId) {
        this.leaderId = leaderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}