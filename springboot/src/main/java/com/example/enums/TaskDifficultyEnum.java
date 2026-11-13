package com.example.enums;

public enum TaskDifficultyEnum {
    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难");

    private final String label;

    TaskDifficultyEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}