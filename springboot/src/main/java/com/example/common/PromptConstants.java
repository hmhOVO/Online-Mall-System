package com.example.common;

/**
 * AI Prompt 常量
 * 统一维护各 AI 场景的 systemPrompt 和 outputSchema，避免依赖数据库 prompt_template 表。
 */
public final class PromptConstants {

    private PromptConstants() {}

    public static final String SCENE_TASK_SPLIT = "TASK_SPLIT";
    public static final String SCENE_RISK_ANALYSIS = "RISK_ANALYSIS";

    public static final String TASK_SPLIT_SYSTEM_PROMPT =
            "你是一名高校实训课程任务拆解专家。请根据用户输入的实训需求、学生人数和实训周期，将实训需求拆解为结构化的任务列表。每个任务需给出简要描述（description）、任务类型（taskType，只能是 COGNITIVE/SKILL/COMPREHENSIVE/DELIVERABLE）和难度（difficulty，只能是 EASY/MEDIUM/HARD）。任务层级最多3级，优先级只能是 HIGH/MEDIUM/LOW。dependencies 字段使用整数数组，表示所依赖任务在返回数组中的索引位置（从0开始），禁止使用任务标题字符串。";

    public static final String TASK_SPLIT_OUTPUT_SCHEMA =
            "{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"},\"description\":{\"type\":\"string\"},\"taskType\":{\"type\":\"string\",\"enum\":[\"COGNITIVE\",\"SKILL\",\"COMPREHENSIVE\",\"DELIVERABLE\"]},\"difficulty\":{\"type\":\"string\",\"enum\":[\"EASY\",\"MEDIUM\",\"HARD\"]},\"priority\":{\"type\":\"string\",\"enum\":[\"HIGH\",\"MEDIUM\",\"LOW\"]},\"plannedHours\":{\"type\":\"number\"},\"dependencies\":{\"type\":\"array\",\"items\":{\"type\":\"integer\",\"minimum\":0}},\"level\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":3}},\"required\":[\"title\",\"description\",\"taskType\",\"difficulty\",\"priority\",\"plannedHours\",\"level\"]}}";

    public static final String RISK_ANALYSIS_SYSTEM_PROMPT =
            "你是一名项目风险分析专家。请严格根据项目统计数据输出风险分析结果，输出必须是纯 JSON，不要包含 markdown 代码块、解释性文字或任何其他内容。" +
            "JSON 结构为：{\"riskLevel\":\"HIGH/MIDDLE/LOW\",\"riskItems\":[\"风险项1\",\"风险项2\"],\"suggestions\":[\"建议1\",\"建议2\"]}。" +
            "如果项目没有任何任务或数据不足，riskLevel 输出 LOW，riskItems 输出 [\"项目暂无有效任务数据，无法评估具体风险\"]，suggestions 输出 [\"请先创建任务并录入进度，再重新进行风险分析\"]。";

    public static final String RISK_ANALYSIS_OUTPUT_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"riskLevel\":{\"type\":\"string\",\"enum\":[\"HIGH\",\"MIDDLE\",\"LOW\"]},\"riskItems\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"suggestions\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"riskLevel\",\"riskItems\",\"suggestions\"]}";

    public static String getSystemPrompt(String scene) {
        return switch (scene) {
            case SCENE_TASK_SPLIT -> TASK_SPLIT_SYSTEM_PROMPT;
            case SCENE_RISK_ANALYSIS -> RISK_ANALYSIS_SYSTEM_PROMPT;
            default -> throw new IllegalArgumentException("未知场景: " + scene);
        };
    }

    public static String getOutputSchema(String scene) {
        return switch (scene) {
            case SCENE_TASK_SPLIT -> TASK_SPLIT_OUTPUT_SCHEMA;
            case SCENE_RISK_ANALYSIS -> RISK_ANALYSIS_OUTPUT_SCHEMA;
            default -> throw new IllegalArgumentException("未知场景: " + scene);
        };
    }
}
