package cn.godlei.blogserver.service.assistant.impl;

import cn.godlei.blogpojo.dto.assistant.AssistantChatRequest;
import cn.godlei.blogpojo.dto.assistant.AssistantMessageDTO;
import cn.godlei.blogpojo.dto.assistant.AssistantPageContextDTO;
import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogserver.service.assistant.AssistantService;
import cn.godlei.blogserver.service.assistant.client.AssistantUpstreamException;
import cn.godlei.blogserver.service.assistant.client.OpenAiCompatibleChatClient;
import cn.godlei.blogserver.service.assistant.config.AssistantExperienceConfigService;
import cn.godlei.blogserver.service.assistant.config.AssistantResolvedRuntimeConfig;
import cn.godlei.blogserver.service.assistant.config.AssistantRuntimeConfigService;
import cn.godlei.blogserver.service.site.SiteConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private static final int MAX_HISTORY_MESSAGES = 12;
    private static final String EMPTY_ASSISTANT_REPLY_MESSAGE = "刚刚这条回复没有成功生成，你可以再问我一次。";

    private final ObjectMapper objectMapper;
    private final SiteConfigService siteConfigService;
    private final AssistantRuntimeConfigService assistantRuntimeConfigService;
    private final AssistantExperienceConfigService assistantExperienceConfigService;
    private final OpenAiCompatibleChatClient openAiCompatibleChatClient;

    private final Map<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();

    @PostConstruct
    public void logAssistantStartupStatus() {
        try {
            AssistantResolvedRuntimeConfig status = assistantRuntimeConfigService.getResolvedRuntimeConfig();
            if (status.isEnabled() && !StringUtils.hasText(status.getApiKey())) {
                log.warn("Assistant is enabled but no API key is configured. provider={}, model={}, baseUrl={}",
                        status.getProvider(), status.getModel(), status.getBaseUrl());
            }
        } catch (Exception ex) {
            log.warn("Inspect assistant runtime status on startup failed", ex);
        }
    }

    @Override
    public void streamChat(AssistantChatRequest request, String clientKey, OutputStream outputStream) throws IOException {
        SiteConfigDTO siteConfig = siteConfigService.getConfig();
        SiteConfigDTO.Assistant assistantConfig = siteConfig.getAssistant();
        AssistantResolvedRuntimeConfig runtimeConfig = assistantRuntimeConfigService.getResolvedRuntimeConfig();

        String sessionId = normalizeText(request == null ? null : request.getSessionId());
        if (!StringUtils.hasText(sessionId)) {
            sessionId = "session-" + System.currentTimeMillis();
        }

        writeEvent(outputStream, "meta", buildMetaPayload(sessionId, assistantConfig, runtimeConfig));

        if (!runtimeConfig.isEnabled()) {
            writeError(outputStream, "AI 助手当前未启用，请先在后台开启。");
            return;
        }

        if (!StringUtils.hasText(runtimeConfig.getApiKey())) {
            writeError(outputStream, "AI 助手服务端尚未配置可用的 API Key。");
            return;
        }

        if (!StringUtils.hasText(runtimeConfig.getBaseUrl())) {
            writeError(outputStream, "AI 助手尚未配置可用的 Base URL。");
            return;
        }

        if (!StringUtils.hasText(runtimeConfig.getModel())) {
            writeError(outputStream, "AI 助手尚未配置可用的模型名。");
            return;
        }

        if (!tryAcquire(clientKey, runtimeConfig.getMaxRequestsPerMinute())) {
            writeError(outputStream, "请求过于频繁，请稍后再试。");
            return;
        }

        List<Map<String, Object>> messages = buildRequestMessages(request);
        if (messages.size() <= 1) {
            writeError(outputStream, "请输入要发送给馨宝的问题。");
            return;
        }

        int totalChars = messages.stream()
                .map(item -> String.valueOf(item.get("content")))
                .mapToInt(String::length)
                .sum();
        if (totalChars > runtimeConfig.getMaxInputChars()) {
            writeError(outputStream, "输入内容过长，请精简后再试。");
            return;
        }

        AtomicBoolean hasContent = new AtomicBoolean(false);
        try {
            openAiCompatibleChatClient.streamChat(runtimeConfig, messages, delta -> {
                if (!StringUtils.hasText(delta)) {
                    return;
                }
                hasContent.set(true);
                try {
                    writeEvent(outputStream, "delta", Map.of("content", delta));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            if (ex instanceof AssistantUpstreamException upstreamException) {
                log.warn("Assistant upstream stream failed, status={}, summary={}",
                        upstreamException.getStatusCode(), upstreamException.getUpstreamSummary());
                writeError(outputStream, "模型服务暂时不可用，请稍后再试。");
                return;
            }
            log.error("Assistant stream proxy failed", ex);
            writeError(outputStream, "AI 助手暂时开小差了，请稍后再试。");
            return;
        } catch (Exception ex) {
            log.error("Assistant stream proxy failed", ex);
            writeError(outputStream, "AI 助手暂时开小差了，请稍后再试。");
            return;
        }

        if (!hasContent.get()) {
            writeError(outputStream, EMPTY_ASSISTANT_REPLY_MESSAGE);
            return;
        }
        writeEvent(outputStream, "done", Map.of("sessionId", sessionId));
    }

    private Map<String, Object> buildMetaPayload(String sessionId,
                                                 SiteConfigDTO.Assistant assistantConfig,
                                                 AssistantResolvedRuntimeConfig runtimeConfig) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("assistantName", assistantConfig == null ? "馨宝" : assistantConfig.getName());
        payload.put("assistantSubtitle", assistantConfig == null ? "站内 AI 助手" : assistantConfig.getSubtitle());
        payload.put("model", runtimeConfig.getModel());
        return payload;
    }

    private List<Map<String, Object>> buildRequestMessages(AssistantChatRequest request) {
        List<Map<String, Object>> result = new ArrayList<>();
        String systemPrompt = assistantExperienceConfigService.resolveSystemPrompt();
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
            return "以下是当前页面的结构化上下文。只有在问题与页面相关时再引用它；若页面未提供信息，请明确说明而不是编造：\n"
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

    private boolean tryAcquire(String clientKey, int maxRequestsPerMinute) {
        String safeKey = StringUtils.hasText(clientKey) ? clientKey : "anonymous";
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        Deque<Long> window = requestWindows.computeIfAbsent(safeKey, key -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < windowStart) {
                window.pollFirst();
            }
            if (window.size() >= maxRequestsPerMinute) {
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

    private String normalizeRole(String role) {
        String normalized = normalizeText(role).toLowerCase();
        if ("user".equals(normalized) || "assistant".equals(normalized)) {
            return normalized;
        }
        return "";
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
