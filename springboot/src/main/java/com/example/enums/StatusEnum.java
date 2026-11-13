package com.example.enums;

public enum StatusEnum {
    ENABLED("启用"),
    DISABLED("禁用");

    private final String label;

    StatusEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
