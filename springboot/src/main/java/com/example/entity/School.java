package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("school_info")
public class School extends BaseEntity {

    private String name;

    private String code;

    private String dean;

    private String contactPhone;

    private String description;

    private Integer sortOrder;

    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDean() {
        return dean;
    }

    public void setDean(String dean) {
        this.dean = dean;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}