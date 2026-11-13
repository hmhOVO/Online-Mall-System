package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ai_call_log")
public class AiCallLog extends BaseEntity {

    public static final String AI_TYPE_DEEPSEEK = "DEEPSEEK";

    private Integer userId;
    private Integer projectId;
    private String aiType;
    private String scene;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private String request;
    private String response;
    private String status;
    private String errorMsg;
    private Long latencyMs;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public String getAiType() { return aiType; }
    public void setAiType(String aiType) { this.aiType = aiType; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public String getRequest() { return request; }
    public void setRequest(String request) { this.request = request; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
}
