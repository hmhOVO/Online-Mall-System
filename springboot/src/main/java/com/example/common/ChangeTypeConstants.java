package com.example.common;

/**
 * 任务变更历史类型常量
 * 与数据库 task_change_log.change_type 的 CHECK 约束保持一致。
 */
public final class ChangeTypeConstants {

    private ChangeTypeConstants() {}

    public static final String UPDATE = "UPDATE";
    public static final String STATUS = "STATUS";
    public static final String ASSIGN = "ASSIGN";
    public static final String PROGRESS = "PROGRESS";
    public static final String REVIEW = "REVIEW";

    public static boolean isValid(String changeType) {
        return UPDATE.equals(changeType)
                || STATUS.equals(changeType)
                || ASSIGN.equals(changeType)
                || PROGRESS.equals(changeType)
                || REVIEW.equals(changeType);
    }
}
