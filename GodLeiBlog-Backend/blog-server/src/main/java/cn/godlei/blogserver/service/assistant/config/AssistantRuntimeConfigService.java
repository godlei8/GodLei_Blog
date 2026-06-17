package cn.godlei.blogserver.service.assistant.config;

import cn.godlei.blogpojo.dto.assistant.AssistantCredentialUpdateRequest;
import cn.godlei.blogpojo.dto.assistant.AssistantRuntimeConfigUpdateRequest;
import cn.godlei.blogpojo.dto.response.assistant.AssistantProviderPresetDTO;
import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeStatusDTO;
import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeTestResultDTO;
import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogpojo.entity.SiteSetting;
import cn.godlei.blogserver.config.AssistantProperties;
import cn.godlei.blogserver.mapper.SiteSettingMapper;
import cn.godlei.blogserver.service.assistant.client.OpenAiCompatibleChatClient;
import cn.godlei.blogserver.service.site.SiteConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantRuntimeConfigService {

    private static final String ASSISTANT_RUNTIME_CONFIG_KEY = "assistant_runtime_config";
    private static final String ASSISTANT_RUNTIME_CONFIG_DESC = "AI 助手运行时配置";
    private static final String ASSISTANT_API_KEY_SETTING_KEY = "assistant_api_key";
    private static final String ASSISTANT_API_KEY_SETTING_DESC = "AI 助手 API Key（后台配置）";
    private static final String DEFAULT_PROVIDER = "deepseek";
    private static final String DEFAULT_MODEL = "deepseek-v4-pro";

    private static final Map<String, ProviderPreset> PROVIDER_PRESETS = createProviderPresets();

    private final ObjectMapper objectMapper;
    private final SiteSettingMapper siteSettingMapper;
    private final SiteConfigService siteConfigService;
    private final AssistantProperties assistantProperties;
    private final AssistantExperienceConfigService assistantExperienceConfigService;
    private final OpenAiCompatibleChatClient openAiCompatibleChatClient;

    public AssistantResolvedRuntimeConfig getResolvedRuntimeConfig() {
        return resolveRuntimeConfig(getStoredRuntimeConfig(), resolveConfiguredApiKey(null, false), resolveRuntimeSource());
    }

    public AssistantRuntimeStatusDTO getRuntimeStatus() {
        AssistantResolvedRuntimeConfig resolved = getResolvedRuntimeConfig();
        AssistantRuntimeStatusDTO status = new AssistantRuntimeStatusDTO();
        status.setEnabled(resolved.isEnabled());
        status.setProvider(resolved.getProvider());
        status.setProviderLabel(resolved.getProviderLabel());
        status.setBaseUrl(resolved.getBaseUrl());
        status.setModel(resolved.getModel());
        status.setTemperature(resolved.getTemperature());
        status.setTopP(resolved.getTopP());
        status.setMaxTokens(resolved.getMaxTokens());
        status.setConnectTimeoutMs(resolved.getConnectTimeoutMs());
        status.setReadTimeoutMs(resolved.getReadTimeoutMs());
        status.setMaxRequestsPerMinute(resolved.getMaxRequestsPerMinute());
        status.setApiKeyConfigured(StringUtils.hasText(resolved.getApiKey()));
        status.setApiKeyMasked(maskApiKey(resolved.getApiKey()));
        status.setApiKeySource(resolved.getApiKeySource());
        status.setRuntimeSource(resolved.getRuntimeSource());
        status.setPresets(buildPresetDTOs());
        return status;
    }

    public AssistantRuntimeStatusDTO saveRuntimeConfig(AssistantRuntimeConfigUpdateRequest request) {
        if (request == null) {
            return getRuntimeStatus();
        }
        StoredRuntimeConfig storedRuntimeConfig = normalizeStoredRuntimeConfig(request);
        validateStoredRuntimeConfig(storedRuntimeConfig);

        SiteSetting runtimeSetting = new SiteSetting();
        runtimeSetting.setSettingKey(ASSISTANT_RUNTIME_CONFIG_KEY);
        runtimeSetting.setSettingDesc(ASSISTANT_RUNTIME_CONFIG_DESC);
        runtimeSetting.setSettingValue(writeRuntimeConfig(storedRuntimeConfig));
        siteSettingMapper.upsert(runtimeSetting);

        updateApiKeyInternal(request == null ? null : request.getApiKey(), request != null && request.isClearApiKey());
        return getRuntimeStatus();
    }

    public AssistantRuntimeTestResultDTO testRuntime(AssistantRuntimeConfigUpdateRequest request) {
        StoredRuntimeConfig runtimeConfig = request == null ? getStoredRuntimeConfig() : normalizeStoredRuntimeConfig(request);
        String apiKey = resolveConfiguredApiKey(request == null ? null : request.getApiKey(), request != null && request.isClearApiKey());
        AssistantResolvedRuntimeConfig resolved = resolveRuntimeConfig(
                runtimeConfig,
                apiKey,
                request == null ? resolveRuntimeSource() : "preview"
        );

        if (!StringUtils.hasText(resolved.getBaseUrl())) {
            return failureResult("缺少 Base URL，请先补全后再测试。", resolved);
        }
        if (!StringUtils.hasText(resolved.getModel())) {
            return failureResult("缺少模型名，请先补全后再测试。", resolved);
        }
        if (!StringUtils.hasText(resolved.getApiKey())) {
            return failureResult("缺少 API Key，请先补全后再测试。", resolved);
        }

        return openAiCompatibleChatClient.testConnection(resolved, assistantExperienceConfigService.resolveSystemPrompt());
    }

    public AssistantRuntimeStatusDTO updateApiKey(String apiKey, boolean clearExisting) {
        updateApiKeyInternal(apiKey, clearExisting);
        return getRuntimeStatus();
    }

    public AssistantRuntimeStatusDTO updateApiKey(AssistantCredentialUpdateRequest request) {
        String apiKey = request == null ? null : request.getApiKey();
        boolean clearExisting = request != null && request.isClearExisting();
        return updateApiKey(apiKey, clearExisting);
    }

    private void updateApiKeyInternal(String apiKey, boolean clearExisting) {
        String normalizedApiKey = normalizeText(apiKey);
        if (!StringUtils.hasText(normalizedApiKey) && !clearExisting) {
            return;
        }
        SiteSetting siteSetting = new SiteSetting();
        siteSetting.setSettingKey(ASSISTANT_API_KEY_SETTING_KEY);
        siteSetting.setSettingDesc(ASSISTANT_API_KEY_SETTING_DESC);
        siteSetting.setSettingValue(clearExisting ? "" : normalizedApiKey);
        siteSettingMapper.upsert(siteSetting);
    }

    private StoredRuntimeConfig getStoredRuntimeConfig() {
        SiteSetting setting = siteSettingMapper.getByKey(ASSISTANT_RUNTIME_CONFIG_KEY);
        if (setting == null || !StringUtils.hasText(setting.getSettingValue())) {
            return new StoredRuntimeConfig();
        }
        try {
            StoredRuntimeConfig config = objectMapper.readValue(setting.getSettingValue(), StoredRuntimeConfig.class);
            return config == null ? new StoredRuntimeConfig() : config;
        } catch (Exception ex) {
            log.warn("Parse assistant runtime config failed", ex);
            return new StoredRuntimeConfig();
        }
    }

    private StoredRuntimeConfig normalizeStoredRuntimeConfig(AssistantRuntimeConfigUpdateRequest request) {
        StoredRuntimeConfig config = new StoredRuntimeConfig();
        if (request == null) {
            return config;
        }
        config.setProvider(normalizeText(request.getProvider()).toLowerCase());
        config.setBaseUrl(normalizeText(request.getBaseUrl()));
        config.setModel(normalizeText(request.getModel()));
        config.setTemperature(request.getTemperature());
        config.setTopP(request.getTopP());
        config.setMaxTokens(request.getMaxTokens());
        config.setConnectTimeoutMs(request.getConnectTimeoutMs());
        config.setReadTimeoutMs(request.getReadTimeoutMs());
        config.setMaxRequestsPerMinute(request.getMaxRequestsPerMinute());
        return config;
    }

    private void validateStoredRuntimeConfig(StoredRuntimeConfig config) {
        String provider = firstNonBlank(config.getProvider(), normalizeOptionalProvider(assistantProperties.getProvider()), DEFAULT_PROVIDER);
        if (!PROVIDER_PRESETS.containsKey(provider)) {
            throw new IllegalArgumentException("暂不支持的模型供应商：" + provider);
        }
        if ("custom".equals(provider) && !StringUtils.hasText(config.getBaseUrl())) {
            throw new IllegalArgumentException("自定义供应商必须填写 Base URL");
        }
    }

    private AssistantResolvedRuntimeConfig resolveRuntimeConfig(StoredRuntimeConfig storedConfig,
                                                               String resolvedApiKey,
                                                               String runtimeSource) {
        String provider = firstNonBlank(
                normalizeOptionalProvider(storedConfig.getProvider()),
                normalizeOptionalProvider(assistantProperties.getProvider()),
                DEFAULT_PROVIDER
        );
        ProviderPreset preset = PROVIDER_PRESETS.getOrDefault(provider, PROVIDER_PRESETS.get(DEFAULT_PROVIDER));
        String baseUrl = firstNonBlank(
                normalizeText(storedConfig.getBaseUrl()),
                normalizeText(assistantProperties.getBaseUrl()),
                preset.getDefaultBaseUrl()
        );
        String model = firstNonBlank(
                normalizeText(storedConfig.getModel()),
                normalizeText(assistantProperties.getModel()),
                DEFAULT_MODEL
        );
        Double temperature = firstNonNull(storedConfig.getTemperature(), assistantProperties.getTemperature(), 0.7D);
        Double topP = firstNonNull(storedConfig.getTopP(), assistantProperties.getTopP(), 0.95D);
        Integer maxTokens = firstNonNull(storedConfig.getMaxTokens(), assistantProperties.getMaxTokens(), null);
        Long connectTimeoutMs = firstPositive(storedConfig.getConnectTimeoutMs(), assistantProperties.getConnectTimeoutMs(), 10000L);
        Long readTimeoutMs = firstPositive(storedConfig.getReadTimeoutMs(), assistantProperties.getReadTimeoutMs(), 120000L);
        Integer maxRequestsPerMinute = firstPositive(storedConfig.getMaxRequestsPerMinute(), assistantProperties.getMaxRequestsPerMinute(), 12);

        SiteConfigDTO siteConfig = siteConfigService.getConfig();
        SiteConfigDTO.Assistant assistantConfig = siteConfig.getAssistant();

        AssistantResolvedRuntimeConfig runtimeConfig = new AssistantResolvedRuntimeConfig();
        runtimeConfig.setEnabled(assistantProperties.isEnabled() && assistantConfig != null && assistantConfig.isEnabled());
        runtimeConfig.setProvider(provider);
        runtimeConfig.setProviderLabel(preset.getLabel());
        runtimeConfig.setBaseUrl(trimTrailingSlash(baseUrl));
        runtimeConfig.setModel(model);
        runtimeConfig.setApiKey(normalizeText(resolvedApiKey));
        runtimeConfig.setApiKeySource(resolveApiKeySource());
        runtimeConfig.setRuntimeSource(runtimeSource);
        runtimeConfig.setTemperature(temperature);
        runtimeConfig.setTopP(topP);
        runtimeConfig.setMaxTokens(maxTokens);
        runtimeConfig.setConnectTimeoutMs(connectTimeoutMs);
        runtimeConfig.setReadTimeoutMs(readTimeoutMs);
        runtimeConfig.setMaxRequestsPerMinute(maxRequestsPerMinute);
        runtimeConfig.setMaxInputChars(assistantProperties.getMaxInputChars());
        return runtimeConfig;
    }

    private String resolveConfiguredApiKey(String runtimeApiKeyOverride, boolean clearApiKey) {
        if (clearApiKey) {
            return "";
        }
        String inlineApiKey = normalizeText(runtimeApiKeyOverride);
        if (StringUtils.hasText(inlineApiKey)) {
            return inlineApiKey;
        }
        String storedApiKey = getStoredApiKey();
        if (StringUtils.hasText(storedApiKey)) {
            return storedApiKey;
        }
        return normalizeText(assistantProperties.getApiKey());
    }

    private String resolveApiKeySource() {
        if (StringUtils.hasText(getStoredApiKey())) {
            return "admin";
        }
        if (StringUtils.hasText(assistantProperties.getApiKey())) {
            return "env";
        }
        return "none";
    }

    private String resolveRuntimeSource() {
        boolean hasStored = getStoredRuntimeConfig().hasMeaningfulValue();
        boolean hasEnv = hasEnvironmentOverride();
        if (hasStored && hasEnv) {
            return "mixed";
        }
        if (hasStored) {
            return "admin";
        }
        if (hasEnv) {
            return "env";
        }
        return "default";
    }

    private boolean hasEnvironmentOverride() {
        return hasEnv("BLOG_ASSISTANT_PROVIDER")
                || hasEnv("BLOG_ASSISTANT_BASE_URL")
                || hasEnv("BLOG_ASSISTANT_MODEL")
                || hasEnv("BLOG_ASSISTANT_TEMPERATURE")
                || hasEnv("BLOG_ASSISTANT_TOP_P")
                || hasEnv("BLOG_ASSISTANT_MAX_TOKENS")
                || hasEnv("BLOG_ASSISTANT_CONNECT_TIMEOUT_MS")
                || hasEnv("BLOG_ASSISTANT_READ_TIMEOUT_MS")
                || hasEnv("BLOG_ASSISTANT_MAX_REQUESTS_PER_MINUTE")
                || hasEnv("BLOG_ASSISTANT_API_KEY")
                || hasEnv("OPENAI_API_KEY");
    }

    private boolean hasEnv(String key) {
        return StringUtils.hasText(System.getenv(key));
    }

    private String getStoredApiKey() {
        SiteSetting siteSetting = siteSettingMapper.getByKey(ASSISTANT_API_KEY_SETTING_KEY);
        if (siteSetting == null) {
            return "";
        }
        return normalizeText(siteSetting.getSettingValue());
    }

    private String writeRuntimeConfig(StoredRuntimeConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ex) {
            throw new IllegalStateException("Serialize assistant runtime config failed", ex);
        }
    }

    private List<AssistantProviderPresetDTO> buildPresetDTOs() {
        List<AssistantProviderPresetDTO> presets = new ArrayList<>();
        for (Map.Entry<String, ProviderPreset> entry : PROVIDER_PRESETS.entrySet()) {
            ProviderPreset preset = entry.getValue();
            presets.add(new AssistantProviderPresetDTO(
                    entry.getKey(),
                    preset.getLabel(),
                    preset.getDefaultBaseUrl(),
                    preset.isCustom()
            ));
        }
        return presets;
    }

    private AssistantRuntimeTestResultDTO failureResult(String message, AssistantResolvedRuntimeConfig config) {
        return new AssistantRuntimeTestResultDTO(
                false,
                0L,
                null,
                message,
                config.getProvider(),
                config.getModel()
        );
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

    private String normalizeProvider(String provider) {
        String normalized = normalizeText(provider).toLowerCase();
        return StringUtils.hasText(normalized) ? normalized : DEFAULT_PROVIDER;
    }

    private String normalizeOptionalProvider(String provider) {
        String normalized = normalizeText(provider).toLowerCase();
        return StringUtils.hasText(normalized) ? normalized : "";
    }

    private String trimTrailingSlash(String value) {
        String normalized = normalizeText(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String first, String second, String third) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return third == null ? "" : third.trim();
    }

    private <T> T firstNonNull(T first, T second, T third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private Long firstPositive(Long first, long second, long third) {
        if (first != null && first > 0) {
            return first;
        }
        if (second > 0) {
            return second;
        }
        return third;
    }

    private Integer firstPositive(Integer first, int second, int third) {
        if (first != null && first > 0) {
            return first;
        }
        if (second > 0) {
            return second;
        }
        return third;
    }

    private static Map<String, ProviderPreset> createProviderPresets() {
        Map<String, ProviderPreset> presets = new LinkedHashMap<>();
        presets.put("openai", new ProviderPreset("OpenAI", "https://api.openai.com/v1", false));
        presets.put("deepseek", new ProviderPreset("DeepSeek", "https://api.deepseek.com/v1", false));
        presets.put("qwen", new ProviderPreset("Qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1", false));
        presets.put("kimi", new ProviderPreset("Kimi", "https://api.moonshot.cn/v1", false));
        presets.put("openrouter", new ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", false));
        presets.put("siliconflow", new ProviderPreset("SiliconFlow", "https://api.siliconflow.cn/v1", false));
        presets.put("custom", new ProviderPreset("Custom", "", true));
        return presets;
    }

    @lombok.Data
    private static class StoredRuntimeConfig {

        private String provider = "";

        private String baseUrl = "";

        private String model = "";

        private Double temperature;

        private Double topP;

        private Integer maxTokens;

        private Long connectTimeoutMs;

        private Long readTimeoutMs;

        private Integer maxRequestsPerMinute;

        private boolean hasMeaningfulValue() {
            return StringUtils.hasText(provider)
                    || StringUtils.hasText(baseUrl)
                    || StringUtils.hasText(model)
                    || temperature != null
                    || topP != null
                    || maxTokens != null
                    || connectTimeoutMs != null
                    || readTimeoutMs != null
                    || maxRequestsPerMinute != null;
        }
    }

    @lombok.Value
    private static class ProviderPreset {
        String label;
        String defaultBaseUrl;
        boolean custom;
    }
}
