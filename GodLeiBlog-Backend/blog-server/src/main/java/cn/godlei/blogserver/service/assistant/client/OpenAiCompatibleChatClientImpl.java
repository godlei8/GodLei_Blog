package cn.godlei.blogserver.service.assistant.client;

import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeTestResultDTO;
import cn.godlei.blogserver.service.assistant.config.AssistantResolvedRuntimeConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiCompatibleChatClientImpl implements OpenAiCompatibleChatClient {

    private final ObjectMapper objectMapper;

    @Override
    public void streamChat(AssistantResolvedRuntimeConfig runtimeConfig,
                           List<Map<String, Object>> messages,
                           Consumer<String> onDelta) {
        HttpRequest request = buildRequest(runtimeConfig, messages, true);
        try {
            HttpResponse<InputStream> response = buildHttpClient(runtimeConfig)
                    .send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = readAll(response.body());
                throw new AssistantUpstreamException(
                        "上游模型服务暂时不可用",
                        response.statusCode(),
                        summarizeUpstreamError(errorBody)
                );
            }
            proxyStream(response.body(), onDelta);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssistantUpstreamException("AI 助手请求已中断", ex);
        } catch (IOException ex) {
            throw new AssistantUpstreamException("AI 助手请求失败", ex);
        }
    }

    @Override
    public AssistantRuntimeTestResultDTO testConnection(AssistantResolvedRuntimeConfig runtimeConfig,
                                                        String systemPrompt) {
        long startedAt = System.currentTimeMillis();
        try {
            List<Map<String, Object>> messages = buildProbeMessages(systemPrompt);
            HttpRequest request = buildRequest(runtimeConfig, messages, false);
            HttpResponse<InputStream> response = buildHttpClient(runtimeConfig)
                    .send(request, HttpResponse.BodyHandlers.ofInputStream());
            long durationMs = System.currentTimeMillis() - startedAt;
            if (response.statusCode() >= 400) {
                String errorBody = readAll(response.body());
                return new AssistantRuntimeTestResultDTO(
                        false,
                        durationMs,
                        response.statusCode(),
                        summarizeUpstreamError(errorBody),
                        runtimeConfig.getProvider(),
                        runtimeConfig.getModel()
                );
            }

            String body = readAll(response.body());
            String summary = extractCompletionSummary(body);
            if (!StringUtils.hasText(summary)) {
                summary = "连接成功，已收到模型响应。";
            }
            return new AssistantRuntimeTestResultDTO(
                    true,
                    durationMs,
                    response.statusCode(),
                    summary,
                    runtimeConfig.getProvider(),
                    runtimeConfig.getModel()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new AssistantRuntimeTestResultDTO(
                    false,
                    System.currentTimeMillis() - startedAt,
                    null,
                    "测试请求已中断",
                    runtimeConfig.getProvider(),
                    runtimeConfig.getModel()
            );
        } catch (Exception ex) {
            log.warn("Assistant runtime connection test failed", ex);
            return new AssistantRuntimeTestResultDTO(
                    false,
                    System.currentTimeMillis() - startedAt,
                    null,
                    ex.getMessage(),
                    runtimeConfig.getProvider(),
                    runtimeConfig.getModel()
            );
        }
    }

    private HttpClient buildHttpClient(AssistantResolvedRuntimeConfig runtimeConfig) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(runtimeConfig.getConnectTimeoutMs()))
                .build();
    }

    private HttpRequest buildRequest(AssistantResolvedRuntimeConfig runtimeConfig,
                                     List<Map<String, Object>> messages,
                                     boolean stream) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", runtimeConfig.getModel());
        payload.put("messages", messages);
        payload.put("stream", stream);
        payload.put("temperature", runtimeConfig.getTemperature());
        payload.put("top_p", runtimeConfig.getTopP());
        if (runtimeConfig.getMaxTokens() != null) {
            payload.put("max_tokens", runtimeConfig.getMaxTokens());
        }

        try {
            return HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(runtimeConfig.getBaseUrl()) + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Accept", stream ? "text/event-stream" : "application/json")
                    .header("Authorization", "Bearer " + runtimeConfig.getApiKey())
                    .timeout(Duration.ofMillis(runtimeConfig.getReadTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception ex) {
            throw new AssistantUpstreamException("构建模型请求失败", ex);
        }
    }

    private List<Map<String, Object>> buildProbeMessages(String systemPrompt) {
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", "Reply with OK."
        );
        if (!StringUtils.hasText(systemPrompt)) {
            return List.of(userMessage);
        }
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                userMessage
        );
    }

    private void proxyStream(InputStream inputStream, Consumer<String> onDelta) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (!StringUtils.hasText(data)) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    return;
                }
                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                JsonNode deltaNode = choices.get(0).path("delta");
                String content = extractContent(deltaNode.path("content"));
                if (StringUtils.hasText(content)) {
                    onDelta.accept(content);
                }
            }
        }
    }

    private String extractCompletionSummary(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return "";
            }
            String content = extractContent(choices.get(0).path("message").path("content"));
            if (StringUtils.hasText(content)) {
                return "连接成功：" + content;
            }
            return "";
        } catch (Exception ex) {
            return "连接成功，已收到模型响应。";
        }
    }

    private String summarizeUpstreamError(String body) {
        if (!StringUtils.hasText(body)) {
            return "上游服务返回了错误响应。";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode errorNode = root.path("error");
            if (errorNode.isObject()) {
                String message = errorNode.path("message").asText("");
                if (StringUtils.hasText(message)) {
                    return message.trim();
                }
                String code = errorNode.path("code").asText("");
                if (StringUtils.hasText(code)) {
                    return code.trim();
                }
            }
            if (root.path("message").isTextual()) {
                return root.path("message").asText("").trim();
            }
        } catch (Exception ex) {
            log.debug("Parse upstream error body failed", ex);
        }
        String compact = body.trim().replaceAll("\\s+", " ");
        return compact.length() > 220 ? compact.substring(0, 220) + "..." : compact;
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item.isTextual()) {
                    builder.append(item.asText(""));
                    continue;
                }
                if ("text".equals(item.path("type").asText(""))) {
                    builder.append(item.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        return "";
    }

    private String readAll(InputStream inputStream) throws IOException {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
