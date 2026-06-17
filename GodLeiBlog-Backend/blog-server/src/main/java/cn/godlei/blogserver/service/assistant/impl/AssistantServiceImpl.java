package cn.godlei.blogserver.service.assistant.impl;

import cn.godlei.blogpojo.dto.assistant.AssistantChatRequest;
import cn.godlei.blogpojo.dto.assistant.AssistantMessageDTO;
import cn.godlei.blogpojo.dto.assistant.AssistantPageContextDTO;
import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeStatusDTO;
import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogpojo.entity.SiteSetting;
import cn.godlei.blogserver.config.AssistantProperties;
import cn.godlei.blogserver.mapper.SiteSettingMapper;
import cn.godlei.blogserver.service.assistant.AssistantService;
import cn.godlei.blogserver.service.site.SiteConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private static final int MAX_HISTORY_MESSAGES = 12;
    private static final String ASSISTANT_API_KEY_SETTING_KEY = "assistant_api_key";
    private static final String ASSISTANT_API_KEY_SETTING_DESC = "AI 助手 MiniMax API Key（后台配置）";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是 GodLei Blog 的站内 AI 助手“馨宝”。回答时请保持自然、准确、简洁；如果页面上下文不足或事实不确定，要明确说明，不要编造。";
    private static final String EMPTY_ASSISTANT_REPLY_MESSAGE = "刚刚这条回复没有成功生成，请再问我一次。";

    private final ObjectMapper objectMapper;
    private final SiteConfigService siteConfigService;
    private final SiteSettingMapper siteSettingMapper;
    private final AssistantProperties assistantProperties;

    private final Map<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();

    @PostConstruct
    public void logAssistantStartupStatus() {
        try {
            AssistantRuntimeStatusDTO status = getRuntimeStatus();
            if (status.isEnabled() && !status.isApiKeyConfigured()) {
                log.warn("Assistant is enabled but no API key is configured in admin settings or env. model={}, baseUrl={}",
                        status.getModel(), status.getBaseUrl());
            }
        } catch (Exception ex) {
            log.warn("Inspect assistant runtime status on startup failed", ex);
        }
    }

    @Override
    public void streamChat(AssistantChatRequest request, String clientKey, OutputStream outputStream) throws IOException {
        SiteConfigDTO siteConfig = siteConfigService.getConfig();
        SiteConfigDTO.Assistant assistantConfig = siteConfig.getAssistant();
        String sessionId = normalizeText(request == null ? null : request.getSessionId());
        if (!StringUtils.hasText(sessionId)) {
            sessionId = "session-" + System.currentTimeMillis();
        }

        writeEvent(outputStream, "meta", buildMetaPayload(sessionId, assistantConfig));

        if (!assistantProperties.isEnabled() || assistantConfig == null || !assistantConfig.isEnabled()) {
            writeError(outputStream, "AI 助手当前未启用，请先在站点配置中开启。");
            return;
        }

        String resolvedApiKey = resolveConfiguredApiKey();
        if (!StringUtils.hasText(resolvedApiKey)) {
            writeError(outputStream, "抱歉，AI 助手服务端未配置 MiniMax API Key。");
            return;
        }

        if (!tryAcquire(clientKey)) {
            writeError(outputStream, "请求过于频繁，请稍后再试。");
            return;
        }

        List<Map<String, Object>> messages = buildRequestMessages(request, assistantConfig);
        if (messages.size() <= 1) {
            writeError(outputStream, "请输入要发送给馨宝的问题。");
            return;
        }

        int totalChars = messages.stream()
                .map(item -> String.valueOf(item.get("content")))
                .mapToInt(String::length)
                .sum();
        if (totalChars > assistantProperties.getMaxInputChars()) {
            writeError(outputStream, "输入内容过长，请精简后再试。");
            return;
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(assistantProperties.getConnectTimeoutMs()))
                .build();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", assistantProperties.getModel());
        payload.put("stream", true);
        payload.put("temperature", 0.7D);
        payload.put("reasoning_split", true);
        payload.put("messages", messages);

        HttpRequest upstreamRequest = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(assistantProperties.getBaseUrl()) + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + resolvedApiKey)
                .timeout(Duration.ofMillis(assistantProperties.getReadTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(upstreamRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = readAll(response.body());
                log.warn("MiniMax assistant stream failed, status={}, body={}", response.statusCode(), errorBody);
                writeError(outputStream, "MiniMax 服务暂时不可用，请稍后重试。");
                return;
            }
            proxyUpstreamStream(sessionId, response.body(), outputStream);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            writeError(outputStream, "AI 助手请求已中断，请稍后重试。");
        } catch (Exception ex) {
            log.error("Assistant stream proxy failed", ex);
            writeError(outputStream, "AI 助手暂时开小差了，请稍后再试。");
        }
    }

    @Override
    public AssistantRuntimeStatusDTO getRuntimeStatus() {
        SiteConfigDTO siteConfig = siteConfigService.getConfig();
        SiteConfigDTO.Assistant assistantConfig = siteConfig == null ? null : siteConfig.getAssistant();
        boolean enabled = assistantProperties.isEnabled() && assistantConfig != null && assistantConfig.isEnabled();
        String storedApiKey = getStoredApiKey();
        String envApiKey = getEnvironmentApiKey();
        String resolvedApiKey = StringUtils.hasText(storedApiKey) ? storedApiKey : envApiKey;

        AssistantRuntimeStatusDTO status = new AssistantRuntimeStatusDTO();
        status.setEnabled(enabled);
        status.setModel(normalizeText(assistantProperties.getModel()));
        status.setApiKeyConfigured(StringUtils.hasText(resolvedApiKey));
        status.setApiKeyMasked(maskApiKey(resolvedApiKey));
        status.setApiKeySource(resolveApiKeySource(storedApiKey, envApiKey));
        status.setBaseUrl(trimTrailingSlash(assistantProperties.getBaseUrl()));
        status.setMaxRequestsPerMinute(assistantProperties.getMaxRequestsPerMinute());
        return status;
    }

    @Override
    public AssistantRuntimeStatusDTO updateApiKey(String apiKey, boolean clearExisting) {
        String normalizedApiKey = normalizeText(apiKey);
        if (!StringUtils.hasText(normalizedApiKey) && !clearExisting) {
            return getRuntimeStatus();
        }

        SiteSetting siteSetting = new SiteSetting();
        siteSetting.setSettingKey(ASSISTANT_API_KEY_SETTING_KEY);
        siteSetting.setSettingDesc(ASSISTANT_API_KEY_SETTING_DESC);
        siteSetting.setSettingValue(clearExisting ? "" : normalizedApiKey);
        siteSettingMapper.upsert(siteSetting);
        return getRuntimeStatus();
    }

    private Map<String, Object> buildMetaPayload(String sessionId, SiteConfigDTO.Assistant assistantConfig) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("assistantName", assistantConfig == null ? "馨宝" : assistantConfig.getName());
        payload.put("assistantSubtitle", assistantConfig == null ? "站内 AI 助手" : assistantConfig.getSubtitle());
        payload.put("model", assistantProperties.getModel());
        return payload;
    }

    private List<Map<String, Object>> buildRequestMessages(AssistantChatRequest request, SiteConfigDTO.Assistant assistantConfig) {
        List<Map<String, Object>> result = new ArrayList<>();
        String systemPrompt = normalizeText(
                assistantConfig == null ? null : assistantConfig.getSystemPrompt(),
                DEFAULT_SYSTEM_PROMPT
        );
        String pageContextPrompt = buildPageContextPrompt(request == null ? null : request.getPageContext());
        if (StringUtils.hasText(pageContextPrompt)) {
            systemPrompt = systemPrompt + "\n\n" + pageContextPrompt;
        }
        result.add(buildMessage("system", systemPrompt));

        List<AssistantMessageDTO> source = request == null || request.getMessages() == null
                ? new ArrayList<>()
                : request.getMessages();
        int start = Math.max(0, source.size() - MAX_HISTORY_MESSAGES);
        for (int i = start; i < source.size(); i++) {
            AssistantMessageDTO item = source.get(i);
            if (item == null) {
                continue;
            }
            String role = normalizeRole(item.getRole());
            String content = normalizeText(item.getContent());
            if (!StringUtils.hasText(role) || !StringUtils.hasText(content)) {
                continue;
            }
            result.add(buildMessage(role, content));
        }
        return result;
    }

    private String buildPageContextPrompt(AssistantPageContextDTO pageContext) {
        if (pageContext == null) {
            return "";
        }
        Map<String, Object> context = new LinkedHashMap<>();
        putIfHasText(context, "pageType", pageContext.getPageType());
        putIfHasText(context, "route", pageContext.getRoute());
        putIfHasText(context, "title", pageContext.getTitle());
        putIfHasText(context, "summary", pageContext.getSummary());
        putIfHasText(context, "contentExcerpt", pageContext.getContentExcerpt());
        if (pageContext.getMomentId() != null) {
            context.put("momentId", pageContext.getMomentId());
        }
        putIfHasText(context, "currentMomentSummary", pageContext.getCurrentMomentSummary());
        if (context.isEmpty()) {
            return "";
        }
        try {
            return "以下是当前页面的结构化上下文。只有在问题与页面相关时再引用它；若页面未提供信息，请明确说明而不是补造事实：\n"
                    + objectMapper.writeValueAsString(context);
        } catch (Exception ex) {
            log.warn("Serialize assistant page context failed", ex);
            return "";
        }
    }

    private Map<String, Object> buildMessage(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private void proxyUpstreamStream(String sessionId, InputStream inputStream, OutputStream outputStream) throws IOException {
        String contentBuffer = "";
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
                    break;
                }

                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                JsonNode delta = choices.get(0).path("delta");
                String content = extractContent(delta.path("content"));
                if (!StringUtils.hasText(content)) {
                    continue;
                }
                String appended = sliceNewContent(contentBuffer, content);
                if (StringUtils.hasText(appended)) {
                    contentBuffer = content;
                    writeEvent(outputStream, "delta", Map.of("content", appended));
                }
            }
        }
        if (!StringUtils.hasText(contentBuffer)) {
            writeError(outputStream, EMPTY_ASSISTANT_REPLY_MESSAGE);
            return;
        }
        writeEvent(outputStream, "done", Map.of("sessionId", sessionId));
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

    private String sliceNewContent(String previous, String current) {
        if (!StringUtils.hasText(previous)) {
            return current;
        }
        if (current.startsWith(previous)) {
            return current.substring(previous.length());
        }
        int prefixLength = 0;
        int max = Math.min(previous.length(), current.length());
        while (prefixLength < max && previous.charAt(prefixLength) == current.charAt(prefixLength)) {
            prefixLength++;
        }
        return current.substring(prefixLength);
    }

    private boolean tryAcquire(String clientKey) {
        String safeKey = StringUtils.hasText(clientKey) ? clientKey : "anonymous";
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        Deque<Long> window = requestWindows.computeIfAbsent(safeKey, key -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < windowStart) {
                window.pollFirst();
            }
            if (window.size() >= assistantProperties.getMaxRequestsPerMinute()) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    private void putIfHasText(Map<String, Object> target, String key, String value) {
        String normalized = normalizeText(value);
        if (StringUtils.hasText(normalized)) {
            target.put(key, normalized);
        }
    }

    private void writeError(OutputStream outputStream, String message) throws IOException {
        writeEvent(outputStream, "error", Map.of("message", message));
    }

    private void writeEvent(OutputStream outputStream, String event, Object data) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("event: ").append(event).append("\n");
        builder.append("data: ").append(objectMapper.writeValueAsString(data)).append("\n\n");
        outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private String readAll(InputStream inputStream) throws IOException {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String normalizeRole(String role) {
        String normalized = normalizeText(role).toLowerCase();
        if ("user".equals(normalized) || "assistant".equals(normalized)) {
            return normalized;
        }
        return "";
    }

    private String getStoredApiKey() {
        SiteSetting siteSetting = siteSettingMapper.getByKey(ASSISTANT_API_KEY_SETTING_KEY);
        if (siteSetting == null) {
            return "";
        }
        return normalizeText(siteSetting.getSettingValue());
    }

    private String getEnvironmentApiKey() {
        return normalizeText(assistantProperties.getApiKey());
    }

    private String resolveConfiguredApiKey() {
        String storedApiKey = getStoredApiKey();
        if (StringUtils.hasText(storedApiKey)) {
            return storedApiKey;
        }
        return getEnvironmentApiKey();
    }

    private String resolveApiKeySource(String storedApiKey, String envApiKey) {
        if (StringUtils.hasText(storedApiKey)) {
            return "admin";
        }
        if (StringUtils.hasText(envApiKey)) {
            return "env";
        }
        return "none";
    }

    private String maskApiKey(String apiKey) {
        String normalized = normalizeText(apiKey);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (normalized.length() <= 8) {
            return normalized.charAt(0) + "***" + normalized.charAt(normalized.length() - 1);
        }
        return normalized.substring(0, 4) + "********" + normalized.substring(normalized.length() - 4);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeText(String value, String fallback) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String trimTrailingSlash(String value) {
        String normalized = normalizeText(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
