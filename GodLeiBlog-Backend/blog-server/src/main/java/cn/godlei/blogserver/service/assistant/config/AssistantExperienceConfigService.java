package cn.godlei.blogserver.service.assistant.config;

import cn.godlei.blogpojo.dto.assistant.AssistantExperienceConfigUpdateRequest;
import cn.godlei.blogpojo.dto.response.assistant.AssistantExperienceConfigDTO;
import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogpojo.entity.SiteSetting;
import cn.godlei.blogserver.mapper.SiteSettingMapper;
import cn.godlei.blogserver.service.site.SiteConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantExperienceConfigService {

    public static final String DEFAULT_SYSTEM_PROMPT = "你是 GodLei Blog 的站内 AI 助手“馨宝”。回答时请保持自然、准确、简洁；如果页面上下文不足或事实不确定，要明确说明，不要编造。";

    private static final String SITE_CONFIG_KEY = "site_config";
    private static final String ASSISTANT_PROMPT_CONFIG_KEY = "assistant_prompt_config";
    private static final String ASSISTANT_PROMPT_CONFIG_DESC = "AI 助手私有提示词配置";

    private final SiteConfigService siteConfigService;
    private final SiteSettingMapper siteSettingMapper;
    private final ObjectMapper objectMapper;

    public AssistantExperienceConfigDTO getExperienceConfig() {
        SiteConfigDTO siteConfig = siteConfigService.getConfig();
        SiteConfigDTO.Assistant assistant = siteConfig.getAssistant();

        AssistantExperienceConfigDTO result = new AssistantExperienceConfigDTO();
        result.setEnabled(assistant.isEnabled());
        result.setName(assistant.getName());
        result.setSubtitle(assistant.getSubtitle());
        result.setWelcomeMessage(assistant.getWelcomeMessage());
        result.setStarterPrompts(new ArrayList<>(assistant.getStarterPrompts()));
        result.setDisclaimer(assistant.getDisclaimer());
        result.setSystemPrompt(resolveSystemPrompt());
        return result;
    }

    public AssistantExperienceConfigDTO saveExperienceConfig(AssistantExperienceConfigUpdateRequest request) {
        AssistantExperienceConfigUpdateRequest safeRequest = request == null
                ? new AssistantExperienceConfigUpdateRequest()
                : request;

        SiteConfigDTO siteConfig = siteConfigService.getConfig();
        SiteConfigDTO.Assistant assistant = siteConfig.getAssistant();
        assistant.setEnabled(safeRequest.isEnabled());
        assistant.setName(normalizeText(safeRequest.getName(), "馨宝"));
        assistant.setSubtitle(normalizeText(safeRequest.getSubtitle(), "站内 AI 助手"));
        assistant.setWelcomeMessage(normalizeOptionalText(
                safeRequest.getWelcomeMessage(),
                "你好，我是 **馨宝**。\n\n我可以结合当前页面内容，陪你一起梳理文章、动态和站点信息。"
        ));
        assistant.setStarterPrompts(normalizeStringList(safeRequest.getStarterPrompts()));
        assistant.setDisclaimer(normalizeOptionalText(
                safeRequest.getDisclaimer(),
                "AI 回复可能存在误差，请结合页面原文和实际情况自行判断。"
        ));
        siteConfig.normalize();
        siteConfigService.saveConfig(siteConfig, false);

        StoredPromptConfig promptConfig = new StoredPromptConfig();
        promptConfig.setSystemPrompt(normalizeOptionalText(safeRequest.getSystemPrompt(), ""));

        SiteSetting promptSetting = new SiteSetting();
        promptSetting.setSettingKey(ASSISTANT_PROMPT_CONFIG_KEY);
        promptSetting.setSettingDesc(ASSISTANT_PROMPT_CONFIG_DESC);
        promptSetting.setSettingValue(writePromptConfig(promptConfig));
        siteSettingMapper.upsert(promptSetting);

        return getExperienceConfig();
    }

    public String resolveSystemPrompt() {
        String storedPrompt = getStoredPromptConfig().getSystemPrompt();
        if (StringUtils.hasText(storedPrompt)) {
            return storedPrompt;
        }
        String legacyPrompt = getLegacySystemPrompt();
        if (StringUtils.hasText(legacyPrompt)) {
            return legacyPrompt;
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    private StoredPromptConfig getStoredPromptConfig() {
        SiteSetting setting = siteSettingMapper.getByKey(ASSISTANT_PROMPT_CONFIG_KEY);
        if (setting == null || !StringUtils.hasText(setting.getSettingValue())) {
            return new StoredPromptConfig();
        }
        try {
            StoredPromptConfig config = objectMapper.readValue(setting.getSettingValue(), StoredPromptConfig.class);
            return config == null ? new StoredPromptConfig() : config;
        } catch (Exception ex) {
            log.warn("Parse assistant prompt config failed", ex);
            return new StoredPromptConfig();
        }
    }

    private String getLegacySystemPrompt() {
        SiteSetting setting = siteSettingMapper.getByKey(SITE_CONFIG_KEY);
        if (setting == null || !StringUtils.hasText(setting.getSettingValue())) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(setting.getSettingValue());
            JsonNode promptNode = root.path("assistant").path("systemPrompt");
            return promptNode.isTextual() ? promptNode.asText("").trim() : "";
        } catch (Exception ex) {
            log.warn("Read legacy assistant.systemPrompt failed", ex);
            return "";
        }
    }

    private String writePromptConfig(StoredPromptConfig promptConfig) {
        try {
            return objectMapper.writeValueAsString(promptConfig);
        } catch (Exception ex) {
            throw new IllegalStateException("Serialize assistant prompt config failed", ex);
        }
    }

    private List<String> normalizeStringList(List<String> source) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (String item : source) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            result.add(item.trim());
        }
        return result;
    }

    private String normalizeText(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String normalizeOptionalText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return value.trim();
    }

    @lombok.Data
    private static class StoredPromptConfig {

        private String systemPrompt = "";
    }
}
