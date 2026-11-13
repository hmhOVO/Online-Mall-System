package com.example.enums;

public enum RoleEnum {
    ADMIN("系统管理员"),
    TEACHER("指导教师"),
    STUDENT("学生");

    private final String label;

    RoleEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
