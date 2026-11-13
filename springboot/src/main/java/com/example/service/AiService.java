package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.CurrentUserContext;
import com.example.common.PromptConstants;
import com.example.entity.*;
import com.example.enums.RoleEnum;
import com.example.exception.CustomerException;
import com.example.mapper.*;
import com.example.utils.DeepSeekClient;
import com.example.utils.SseEmitterHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private final DeepSeekClient deepSeekClient;
    private final TaskService taskService;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final TaskMapper taskMapper;
    private final ProjectOperationLogService projectOperationLogService;
    private final AiCallLogMapper aiCallLogMapper;
    private final SysConfigMapper sysConfigMapper;
    private final AiRateLimiterService rateLimiterService;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final com.example.manager.SseEmitterManager emitterManager;
    private final NotificationWebSocketService notificationWebSocketService;

    public AiService(DeepSeekClient deepSeekClient, TaskService taskService,
                     ProjectMapper projectMapper, ProjectMemberMapper projectMemberMapper,
                     TaskMapper taskMapper, ProjectOperationLogService projectOperationLogService,
                     AiCallLogMapper aiCallLogMapper,
                     SysConfigMapper sysConfigMapper, AiRateLimiterService rateLimiterService,
                     UserMapper userMapper, NotificationService notificationService,
                     com.example.manager.SseEmitterManager emitterManager,
                     NotificationWebSocketService notificationWebSocketService) {
        this.deepSeekClient = deepSeekClient;
        this.taskService = taskService;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.taskMapper = taskMapper;
        this.projectOperationLogService = projectOperationLogService;
        this.aiCallLogMapper = aiCallLogMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.rateLimiterService = rateLimiterService;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.emitterManager = emitterManager;
        this.notificationWebSocketService = notificationWebSocketService;
    }

    public List<Task> previewSplitTasks(Integer projectId, String requirement, Integer studentCount, String deadline) {
        User current = CurrentUserContext.get();
        checkProjectAccess(projectId, current);
        rateLimiterService.checkLimit(current.getId(), "task_split");

        String scene = PromptConstants.SCENE_TASK_SPLIT;
        String userPrompt = buildSplitPrompt(projectId, requirement, studentCount, deadline);

        long start = System.currentTimeMillis();
        JsonNode result;
        try {
            // 单轮生成：快速拆解，不做反思/修正
            result = deepSeekClient.chat(
                    PromptConstants.getSystemPrompt(scene), userPrompt, PromptConstants.getOutputSchema(scene));
            validateSplitResult(result);
        } catch (Exception e) {
            logAiCall(current.getId(), projectId, scene, null, null, "FAILED", e.getMessage(), System.currentTimeMillis() - start, userPrompt, null);
            throw e;
        }

        if (!result.isArray()) {
            throw new CustomerException("AI 返回结果格式错误");
        }
        List<Task> previewTasks = new ArrayList<>(result.size());
        for (JsonNode node : result) {
            Task task = new Task();
            task.setProjectId(projectId);
            task.setTitle(node.path("title").asText());
            task.setDescription(node.path("description").asText("AI 自动生成：" + requirement));
            task.setTaskType(node.path("taskType").asText("SKILL"));
            task.setDifficulty(node.path("difficulty").asText("MEDIUM"));
            task.setPriority(node.path("priority").asText("MEDIUM"));
            task.setPlannedHours(node.path("plannedHours").asDouble(0));
            task.setTaskLevel(node.path("level").asInt(1));
            task.setStartDate(LocalDate.now());
            task.setEndDate(parseDeadline(deadline));
            task.setCreatorId(current.getId());
            task.setStatus("TODO");
            task.setProgress(0);
            List<Integer> deps = new ArrayList<>();
            JsonNode depsNode = node.path("dependencies");
            if (depsNode.isArray()) {
                for (JsonNode dep : depsNode) {
                    deps.add(dep.asInt(-1));
                }
            }
            task.setDependencyIds(deps);
            previewTasks.add(task);
        }

        logAiCall(current.getId(), projectId, scene, null, null, "SUCCESS", null,
                System.currentTimeMillis() - start, userPrompt, result.toString());
        log.info("AI 任务拆解完成，任务数量：{}", previewTasks.size());
        return previewTasks;
    }

    public SseEmitter streamSplitTasks(Integer projectId, String requirement, Integer studentCount, String deadline) {
        User current = CurrentUserContext.get();
        checkProjectAccess(projectId, current);
        rateLimiterService.checkLimit(current.getId(), "task_split");

        String scene = PromptConstants.SCENE_TASK_SPLIT;
        String userPrompt = buildSplitPrompt(projectId, requirement, studentCount, deadline);
        SseEmitter emitter = SseEmitterHelper.create();
        emitterManager.register(emitter, current.getId());
        long start = System.currentTimeMillis();

        new Thread(() -> {
            try {
                String sceneInner = PromptConstants.SCENE_TASK_SPLIT;
                SseEmitterHelper.send(emitter, "[PROGRESS]正在生成任务清单...");

                // 单轮生成：快速拆解，不做反思/修正
                JsonNode result = deepSeekClient.chat(
                        PromptConstants.getSystemPrompt(sceneInner), userPrompt, PromptConstants.getOutputSchema(sceneInner));
                validateSplitResult(result);
                SseEmitterHelper.send(emitter, "[PROGRESS]任务清单生成完成，正在组装结果...");

                if (!result.isArray()) {
                    throw new CustomerException("AI 返回结果格式错误");
                }

                // 为每个任务补充前端预览需要的字段（description、status、startDate、endDate 等）
                ArrayNode previewArray = MAPPER.createArrayNode();
                LocalDate deadlineDate = parseDeadline(deadline);
                for (JsonNode node : result) {
                    ObjectNode taskNode = MAPPER.createObjectNode();
                    taskNode.put("title", node.path("title").asText());
                    taskNode.put("description", node.path("description").asText("AI 自动生成：" + requirement));
                    taskNode.put("taskType", node.path("taskType").asText("SKILL"));
                    taskNode.put("difficulty", node.path("difficulty").asText("MEDIUM"));
                    taskNode.put("priority", node.path("priority").asText("MEDIUM"));
                    taskNode.put("plannedHours", node.path("plannedHours").asDouble(0));
                    taskNode.put("taskLevel", node.path("level").asInt(1));
                    taskNode.put("startDate", LocalDate.now().toString());
                    taskNode.put("endDate", deadlineDate.toString());
                    taskNode.put("status", "TODO");
                    taskNode.put("progress", 0);
                    taskNode.set("dependencyIds", node.path("dependencies"));
                    previewArray.add(taskNode);
                }

                logAiCall(current.getId(), projectId, scene, null, null, "SUCCESS", null,
                        System.currentTimeMillis() - start, userPrompt, result.toString());

                SseEmitterHelper.send(emitter, "[RESULT]" + previewArray.toString());
                SseEmitterHelper.complete(emitter);
            } catch (Exception e) {
                log.error("AI 任务拆解流式调用失败", e);
                logAiCall(current.getId(), projectId, scene, null, null, "FAILED", e.getMessage(),
                        System.currentTimeMillis() - start, userPrompt, null);
                SseEmitterHelper.sendError(emitter, e.getMessage());
                SseEmitterHelper.complete(emitter);
            }
        }, "ai-split-" + current.getId()).start();

        return emitter;
    }

    @Transactional
    public List<Task> confirmSplitTasks(Integer projectId, List<Task> tasks) {
        User current = CurrentUserContext.get();
        checkProjectAccess(projectId, current);
        if (tasks == null || tasks.isEmpty()) {
            throw new CustomerException("任务列表不能为空");
        }
        // 先保存原始依赖索引，创建时清空避免引用尚未生成的任务 ID
        List<List<Integer>> originalDepIndices = new ArrayList<>(tasks.size());
        List<Task> created = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            originalDepIndices.add(task.getDependencyIds() == null ? Collections.emptyList() : new ArrayList<>(task.getDependencyIds()));
            task.setProjectId(projectId);
            if (task.getCreatorId() == null) {
                task.setCreatorId(current.getId());
            }
            task.setStatus("TODO");
            task.setProgress(0);
            task.setDependencyIds(Collections.emptyList());
            taskService.createTask(task);
            created.add(task);
        }
        // 处理依赖关系：dependencyIds 为任务在传入数组中的索引
        for (int i = 0; i < tasks.size(); i++) {
            List<Integer> depIndices = originalDepIndices.get(i);
            if (depIndices != null && !depIndices.isEmpty()) {
                List<Integer> depIds = new ArrayList<>();
                for (Integer depIndex : depIndices) {
                    if (depIndex == null || depIndex < 0 || depIndex >= created.size() || depIndex == i) {
                        log.warn("AI 任务拆解依赖索引无效或越界：taskIndex={}, depIndex={}", i, depIndex);
                        continue;
                    }
                    depIds.add(created.get(depIndex).getId());
                }
                if (!depIds.isEmpty()) {
                    Task task = created.get(i);
                    task.setDependencyIds(depIds);
                    taskService.updateTask(task.getId(), task);
                }
            }
        }
        saveProjectLog(projectId, current.getId(), "AI任务拆解",
                "确认导入 " + created.size() + " 个任务：" + created.stream().map(Task::getTitle).collect(Collectors.joining(", ")));
        return created;
    }

    public SseEmitter analyzeRisk(Integer projectId) {
        User current = CurrentUserContext.get();
        checkProjectAccess(projectId, current);
        rateLimiterService.checkLimit(current.getId(), "risk_analyze");

        String scene = PromptConstants.SCENE_RISK_ANALYSIS;
        String userPrompt = buildRiskPrompt(projectId);
        SseEmitter emitter = SseEmitterHelper.create();
        emitterManager.register(emitter, current.getId());
        StringBuilder sb = new StringBuilder();
        long start = System.currentTimeMillis();

        deepSeekClient.streamChat(PromptConstants.getSystemPrompt(scene), userPrompt,
                PromptConstants.getOutputSchema(scene),
                chunk -> {
                    sb.append(chunk);
                    SseEmitterHelper.send(emitter, chunk);
                },
                () -> {
                    String fullResponse = sb.toString();
                    String finalResponse;
                    boolean valid;
                    try {
                        validateRiskResult(fullResponse);
                        finalResponse = fullResponse;
                        valid = true;
                    } catch (Exception e) {
                        log.warn("AI 风险分析输出校验失败，已使用兜底结果。原始响应：{}", fullResponse, e);
                        finalResponse = buildDefaultRiskResponse(fullResponse);
                        valid = false;
                    }
                    saveProjectLog(projectId, current.getId(), "AI风险分析", finalResponse);
                    logAiCall(current.getId(), projectId, scene, null, null, valid ? "SUCCESS" : "FAILED",
                            valid ? null : "AI 输出格式不符合要求，已使用兜底结果",
                            System.currentTimeMillis() - start, userPrompt, fullResponse);
                    if (!valid) {
                        SseEmitterHelper.send(emitter, "\n\n[SYSTEM]AI 返回格式异常，已自动转换为默认风险分析结果。");
                    }
                    SseEmitterHelper.complete(emitter);
                    notifyAiRiskCompleted(projectId, current.getId());
                },
                e -> {
                    logAiCall(current.getId(), projectId, scene, null, null, "FAILED", e.getMessage(),
                            System.currentTimeMillis() - start, userPrompt, null);
                    SseEmitterHelper.sendError(emitter, e.getMessage());
                    SseEmitterHelper.complete(emitter);
                });
        return emitter;
    }

    public void interruptAi() {
        Integer userId = CurrentUserContext.getUserId();
        emitterManager.interruptByUserId(userId);
    }

    public Map<String, Object> getLogStats() {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> todayStats = aiCallLogMapper.selectStatsSince(todayStart);
        Map<String, Object> totalStats = aiCallLogMapper.selectStatsSince(LocalDateTime.of(2000, 1, 1, 0, 0));

        result.put("todayCount", todayStats.get("totalCount"));
        result.put("todaySuccessCount", todayStats.get("successCount"));
        result.put("todayFailCount", todayStats.get("failCount"));
        result.put("todayTokens", todayStats.get("totalTokens"));
        result.put("todayAvgLatency", todayStats.get("avgLatency"));

        result.put("totalCount", totalStats.get("totalCount"));
        result.put("totalSuccessCount", totalStats.get("successCount"));
        result.put("totalFailCount", totalStats.get("failCount"));
        result.put("totalTokens", totalStats.get("totalTokens"));
        result.put("totalAvgLatency", totalStats.get("avgLatency"));

        long total = ((Number) (totalStats.get("totalCount") == null ? 0 : totalStats.get("totalCount"))).longValue();
        long success = ((Number) (totalStats.get("successCount") == null ? 0 : totalStats.get("successCount"))).longValue();
        double successRate = total == 0 ? 0 : (double) success / total * 100;
        result.put("successRate", Math.round(successRate * 10) / 10.0);

        return result;
    }

    public Page<AiCallLog> queryLogs(Integer pageNum, Integer pageSize, Integer userId,
                                      Integer projectId, String scene, String status, String aiType) {
        LambdaQueryWrapper<AiCallLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(userId != null, AiCallLog::getUserId, userId)
                .eq(projectId != null, AiCallLog::getProjectId, projectId)
                .eq(status != null && !status.isBlank(), AiCallLog::getStatus, status)
                .eq(aiType != null && !aiType.isBlank(), AiCallLog::getAiType, aiType)
                .orderByDesc(AiCallLog::getCreatedAt);

        wrapper.eq(scene != null && !scene.isBlank(), AiCallLog::getScene, scene);
        return aiCallLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    private void checkProjectAccess(Integer projectId, User current) {
        if (RoleEnum.ADMIN.name().equals(current.getRole())) return;
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new CustomerException("项目不存在");
        if (Objects.equals(project.getOwnerId(), current.getId())) return;
        if (Objects.equals(project.getTeacherId(), current.getId())) return;
        List<ProjectMember> members = projectMemberMapper.selectByProjectId(projectId);
        boolean member = members.stream().anyMatch(m -> m.getUserId().equals(current.getId()));
        if (!member) throw new CustomerException("您无权操作该项目");
    }

    private String buildSplitPrompt(Integer projectId, String requirement, Integer studentCount, String deadline) {
        Project project = projectMapper.selectById(projectId);
        return "实训项目：" + project.getName() + "\n实训需求：" + requirement
                + "\n学生人数：" + studentCount + "\n实训周期（截止日期）：" + deadline
                + "\n请按模板要求输出结构化任务列表。";
    }

    private String buildRiskPrompt(Integer projectId) {
        List<Task> tasks = taskMapper.selectByProjectId(projectId);
        int total = tasks.size();
        long completed = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        long overdue = tasks.stream().filter(this::isOverdue).count();
        double completedRate = total == 0 ? 0 : completed * 100.0 / total;
        double overdueRate = total == 0 ? 0 : overdue * 100.0 / total;
        double avgDelay = tasks.stream().filter(t -> isOverdue(t) && t.getEndDate() != null)
                .mapToLong(t -> ChronoUnit.DAYS.between(t.getEndDate(), LocalDate.now()))
                .average().orElse(0);
        Map<Integer, Long> loads = tasks.stream().filter(t -> t.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Task::getAssigneeId, Collectors.counting()));
        StringBuilder memberLoads = new StringBuilder();
        loads.forEach((uid, count) -> {
            User u = userMapper.selectById(uid);
            memberLoads.append(u == null ? uid : u.getUsername()).append(":").append(count).append(";");
        });
        return String.format("总任务数：%d，完成率：%.2f%%，逾期率：%.2f%%，平均逾期天数：%.2f，成员负载：%s",
                total, completedRate, overdueRate, avgDelay, memberLoads);
    }

    private boolean isOverdue(Task task) {
        return task.getEndDate() != null
                && task.getEndDate().isBefore(LocalDate.now())
                && !"DONE".equals(task.getStatus());
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text == null ? "" : text;
        return text.substring(0, maxLen) + "...";
    }

    private void saveProjectLog(Integer projectId, Integer operatorId, String action, String detail) {
        User current = CurrentUserContext.get();
        projectOperationLogService.saveLog(projectId, operatorId,
                current == null ? null : current.getUsername(), action,
                detail.length() > 2000 ? detail.substring(0, 2000) : detail);
    }

    private void logAiCall(Integer userId, Integer projectId, String scene,
                           Integer inputTokens, Integer outputTokens, String status,
                           String errorMsg, Long latencyMs, String request, String response) {
        AiCallLog log = new AiCallLog();
        log.setUserId(userId);
        log.setProjectId(projectId);
        log.setAiType(AiCallLog.AI_TYPE_DEEPSEEK);
        log.setScene(scene);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setTotalTokens((inputTokens == null ? 0 : inputTokens) + (outputTokens == null ? 0 : outputTokens));
        log.setRequest(request);
        log.setResponse(response);
        log.setStatus(status);
        log.setErrorMsg(errorMsg);
        log.setLatencyMs(latencyMs);
        aiCallLogMapper.insert(log);
    }

    private void validateSplitResult(JsonNode result) {
        if (result == null || !result.isArray()) {
            throw new CustomerException("AI 任务拆解结果不是数组");
        }
        Set<String> validPriorities = Set.of("HIGH", "MEDIUM", "LOW");
        int size = result.size();
        for (int i = 0; i < size; i++) {
            JsonNode node = result.get(i);
            if (node == null || !node.isObject()) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项不是对象");
            }
            JsonNode titleNode = node.path("title");
            if (!titleNode.isTextual() || titleNode.asText().trim().isEmpty()) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项任务标题为空");
            }
            JsonNode priorityNode = node.path("priority");
            if (!priorityNode.isTextual() || !validPriorities.contains(priorityNode.asText())) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项优先级非法：" + priorityNode.asText());
            }
            JsonNode taskTypeNode = node.path("taskType");
            if (!taskTypeNode.isTextual() || !Set.of("COGNITIVE", "SKILL", "COMPREHENSIVE", "DELIVERABLE").contains(taskTypeNode.asText())) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项任务类型非法：" + taskTypeNode.asText());
            }
            JsonNode difficultyNode = node.path("difficulty");
            if (!difficultyNode.isTextual() || !Set.of("EASY", "MEDIUM", "HARD").contains(difficultyNode.asText())) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项难度非法：" + difficultyNode.asText());
            }
            JsonNode hoursNode = node.path("plannedHours");
            if (!hoursNode.isNumber() || hoursNode.asDouble() < 0) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项计划学时非法");
            }
            JsonNode levelNode = node.path("level");
            if (!levelNode.isInt() || levelNode.asInt() < 1 || levelNode.asInt() > 3) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项任务层级非法");
            }
            JsonNode depsNode = node.path("dependencies");
            if (!depsNode.isMissingNode() && !depsNode.isNull() && !depsNode.isArray()) {
                throw new CustomerException("AI 返回的第 " + (i + 1) + " 项依赖字段必须是数组");
            }
            if (depsNode.isArray()) {
                for (JsonNode dep : depsNode) {
                    if (!dep.isInt()) {
                        throw new CustomerException("AI 返回的第 " + (i + 1) + " 项依赖索引必须是整数");
                    }
                    int depIndex = dep.asInt();
                    if (depIndex < 0 || depIndex >= size) {
                        throw new CustomerException("AI 返回的第 " + (i + 1) + " 项依赖索引越界：" + depIndex);
                    }
                    if (depIndex == i) {
                        throw new CustomerException("AI 返回的第 " + (i + 1) + " 项不能依赖自身");
                    }
                }
            }
        }
    }

    private void validateRiskResult(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new CustomerException("AI 风险分析结果为空");
        }
        String jsonText = response.trim();
        if (jsonText.startsWith("```")) {
            int start = jsonText.indexOf('{');
            int end = jsonText.lastIndexOf('}');
            if (start == -1 || end == -1 || start > end) {
                throw new CustomerException("无法从 markdown 代码块中提取 JSON");
            }
            jsonText = jsonText.substring(start, end + 1);
        }
        try {
            JsonNode root = MAPPER.readTree(jsonText);
            if (!root.isObject()) {
                throw new CustomerException("AI 风险分析结果不是 JSON 对象");
            }
            JsonNode riskLevel = root.path("riskLevel");
            if (!riskLevel.isTextual() || !Set.of("HIGH", "MIDDLE", "LOW").contains(riskLevel.asText())) {
                throw new CustomerException("riskLevel 字段非法");
            }
            JsonNode riskItems = root.path("riskItems");
            if (!riskItems.isArray()) {
                throw new CustomerException("riskItems 字段必须是数组");
            }
            JsonNode suggestions = root.path("suggestions");
            if (!suggestions.isArray()) {
                throw new CustomerException("suggestions 字段必须是数组");
            }
        } catch (CustomerException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomerException("AI 风险分析结果不是合法 JSON：" + e.getMessage());
        }
    }

    private String buildDefaultRiskResponse(String rawResponse) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("riskLevel", "LOW");
            ArrayNode riskItems = root.putArray("riskItems");
            riskItems.add("AI 返回结果无法解析为结构化数据");
            riskItems.add(rawResponse != null && !rawResponse.isBlank() ? "原始分析内容：" + truncate(rawResponse, 500) : "暂无原始分析内容");
            ArrayNode suggestions = root.putArray("suggestions");
            suggestions.add("请检查项目是否已创建任务并录入进度");
            suggestions.add("如问题持续存在，请联系管理员检查 AI 输出格式");
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"riskLevel\":\"LOW\",\"riskItems\":[\"AI 输出解析失败\"],\"suggestions\":[\"请稍后重试\"]}";
        }
    }

    private LocalDate parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) return LocalDate.now().plusDays(7);
        try {
            return LocalDate.parse(deadline);
        } catch (Exception e) {
            return LocalDate.now().plusDays(7);
        }
    }

    private void notifyAiRiskCompleted(Integer projectId, Integer operatorId) {
        try {
            Project project = projectMapper.selectById(projectId);
            if (project == null) return;
            notificationService.send(operatorId, "AI_RISK", "AI 风险报告生成完成",
                    String.format("项目「%s」的 AI 风险分析报告已生成", project.getName()),
                    "/project/" + projectId);
        } catch (Exception e) {
            log.warn("AI风险报告通知发送失败", e);
        }
    }
}
