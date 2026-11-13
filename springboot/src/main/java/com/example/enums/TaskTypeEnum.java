package com.example.enums;

public enum TaskTypeEnum {
    COGNITIVE("认知"),
    SKILL("实操"),
    COMPREHENSIVE("综合"),
    DELIVERABLE("成果");

    private final String label;

    TaskTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}