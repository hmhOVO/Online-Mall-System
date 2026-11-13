package com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class CopyProjectRequest {

    private String name;
    private Integer schoolId;
    private Integer majorId;
    private Integer classId;
    private Integer teacherId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Boolean copyMembers;
    private Boolean copyTasks;
    private Boolean copyGroups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Integer schoolId) {
        this.schoolId = schoolId;
    }

    public Integer getMajorId() {
        return majorId;
    }

    public void setMajorId(Integer majorId) {
        this.majorId = majorId;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
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

    public Boolean getCopyMembers() {
        return copyMembers;
    }

    public void setCopyMembers(Boolean copyMembers) {
        this.copyMembers = copyMembers;
    }

    public Boolean getCopyTasks() {
        return copyTasks;
    }

    public void setCopyTasks(Boolean copyTasks) {
        this.copyTasks = copyTasks;
    }

    public Boolean getCopyGroups() {
        return copyGroups;
    }

    public void setCopyGroups(Boolean copyGroups) {
        this.copyGroups = copyGroups;
    }
}
