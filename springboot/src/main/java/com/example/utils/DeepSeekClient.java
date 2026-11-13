package com.example.utils;

import com.example.config.DeepSeekProperties;
import com.example.exception.CustomerException;
import com.example.service.SysConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DeepSeekProperties properties;
    private final SysConfigService sysConfigService;
    private HttpClient httpClient;

    public DeepSeekClient(DeepSeekProperties properties, SysConfigService sysConfigService) {
        this.properties = properties;
        this.sysConfigService = sysConfigService;
    }

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeout()))
                .build();
    }

    public JsonNode chat(String systemPrompt, String userPrompt, String outputSchema) {
        if (!properties.isEnabled()) {
            throw new CustomerException("AI 功能已禁用");
        }
        checkApiKey();
        String fullSystem = systemPrompt;
        if (outputSchema != null && !outputSchema.isBlank()) {
            fullSystem = systemPrompt + "\n请严格按如下 JSON Schema 输出纯 JSON，不要包含 markdown 代码块：\n" + outputSchema;
        }
        String body = buildRequestBody(fullSystem, userPrompt, false);
        String responseText = executeWithRetry(body, false);
        return parseContentJson(responseText);
    }

    public void streamChat(String systemPrompt, String userPrompt, String outputSchema,
                           Consumer<String> onChunk, Runnable onComplete, Consumer<Exception> onError) {
        if (!properties.isEnabled()) {
            onError.accept(new CustomerException("AI 功能已禁用"));
            return;
        }
        if (!StringUtils.hasText(getApiKey())) {
            onError.accept(new CustomerException("未配置 DeepSeek API Key，请在 AI系统配置中设置"));
            return;
        }
        String fullSystem = systemPrompt;
        if (outputSchema != null && !outputSchema.isBlank()) {
            fullSystem = systemPrompt + "\n请严格按如下 JSON Schema 输出纯 JSON，不要包含 markdown 代码块：\n" + outputSchema;
        }
        String body = buildRequestBody(fullSystem, userPrompt, true);
        log.info("DeepSeek 流式请求体：{}", body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getChatUrl()))
                .header("Authorization", "Bearer " + getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(properties.getTimeout()))
                .build();

        SecurityContext securityContext = SecurityContextHolder.getContext();
        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                        .thenAccept(resp -> {
                            int code = resp.statusCode();
                            if (code == 401) {
                                throw new CustomerException("DeepSeek API Key 无效");
                            }
                            if (code == 429) {
                                throw new CustomerException("DeepSeek 请求过于频繁，请稍后再试");
                            }
                            if (code < 200 || code >= 300) {
                                throw new CustomerException("DeepSeek 请求失败，HTTP " + code);
                            }
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("data: ")) {
                                        String data = line.substring(6).trim();
                                        if ("[DONE]".equals(data)) continue;
                                        JsonNode node = MAPPER.readTree(data);
                                        JsonNode delta = node.path("choices").get(0).path("delta").path("content");
                                        if (!delta.isMissingNode() && !delta.isNull()) {
                                            onChunk.accept(delta.asText());
                                        }
                                    }
                                }
                                onComplete.run();
                            } catch (Exception e) {
                                throw new CustomerException("SSE 流读取失败：" + e.getMessage());
                            }
                        }).get(properties.getTimeout() + 5, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("SSE 请求异常", e);
                onError.accept(e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private String executeWithRetry(String body, boolean stream) {
        int retries = properties.getMaxRetries();
        for (int i = 0; i <= retries; i++) {
            try {
                return executeOnce(body, stream);
            } catch (CustomerException e) {
                throw e;
            } catch (Exception e) {
                log.warn("DeepSeek 调用失败，第{}次重试", i + 1, e);
                if (i == retries) {
                    throw new CustomerException("DeepSeek 调用失败：" + e.getMessage());
                }
                try {
                    Thread.sleep((long) Math.pow(2, i) * 1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new CustomerException("DeepSeek 调用失败");
    }

    private String executeOnce(String body, boolean stream) throws Exception {
        log.info("DeepSeek 请求体：{}", body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getChatUrl()))
                .header("Authorization", "Bearer " + getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(properties.getTimeout()))
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int code = resp.statusCode();
        String respBody = resp.body();
        if (code == 401) throw new CustomerException("DeepSeek API Key 无效：" + respBody);
        if (code == 429) throw new CustomerException("DeepSeek 请求过于频繁，请稍后再试：" + respBody);
        if (code < 200 || code >= 300) {
            log.error("DeepSeek 请求失败，HTTP {}，响应：{}", code, respBody);
            throw new CustomerException("DeepSeek 请求失败，HTTP " + code + "：" + respBody);
        }
        return respBody;
    }

    private String getChatUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/chat/completions";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private void checkApiKey() {
        if (!StringUtils.hasText(getApiKey())) {
            throw new CustomerException("未配置 DeepSeek API Key，请在 AI系统配置中设置");
        }
    }

    private String getApiKey() {
        try {
            if (sysConfigService != null) {
                var config = sysConfigService.getByKey("deepseek.api_key");
                if (config != null && StringUtils.hasText(config.getConfigValue())) {
                    return config.getConfigValue();
                }
            }
        } catch (Exception e) {
            log.warn("读取 sys_config deepseek.api_key 失败，使用配置文件默认值", e);
        }
        return properties.getApiKey();
    }

    private String buildRequestBody(String systemPrompt, String userPrompt, boolean stream) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", properties.getModel());
        root.put("stream", stream);
        root.set("messages", MAPPER.createArrayNode()
                .add(MAPPER.createObjectNode().put("role", "system").put("content", systemPrompt))
                .add(MAPPER.createObjectNode().put("role", "user").put("content", userPrompt)));
        return root.toString();
    }

    private String parseContentText(String responseText) {
        try {
            JsonNode root = MAPPER.readTree(responseText);
            return root.path("choices").get(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.warn("VL 返回解析失败", e);
            throw new CustomerException("VL 返回结果解析失败");
        }
    }

    private JsonNode parseContentJson(String responseText) {
        try {
            JsonNode root = MAPPER.readTree(responseText);
            JsonNode content = root.path("choices").get(0).path("message").path("content");
            return MAPPER.readTree(content.asText());
        } catch (Exception e) {
            log.warn("AI 返回非 JSON，尝试兜底解析", e);
            try {
                String text = responseText.replaceAll("(?s).*```json\\s*(.*?)\\s*```.*", "$1").trim();
                if (text.isBlank()) text = responseText;
                return MAPPER.readTree(text);
            } catch (Exception ex) {
                throw new CustomerException("AI 返回结果无法解析为 JSON");
            }
        }
    }

}
