package cn.godlei.blogserver.service.assistant.config;

import cn.godlei.blogpojo.dto.assistant.AssistantRuntimeConfigUpdateRequest;
import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeStatusDTO;
import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogpojo.entity.SiteSetting;
import cn.godlei.blogserver.config.AssistantProperties;
import cn.godlei.blogserver.mapper.SiteSettingMapper;
import cn.godlei.blogserver.service.assistant.client.OpenAiCompatibleChatClient;
import cn.godlei.blogserver.service.site.SiteConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantRuntimeConfigServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPreferDbRuntimeConfigOverEnvDefaults() throws Exception {
        InMemorySiteSettingMapper mapper = new InMemorySiteSettingMapper();
        mapper.put("assistant_runtime_config", "{\"provider\":\"qwen\",\"baseUrl\":\"https://db.example.com/v1\",\"model\":\"db-model\",\"temperature\":0.4,\"topP\":0.8,\"maxTokens\":4096,\"connectTimeoutMs\":9000,\"readTimeoutMs\":60000,\"maxRequestsPerMinute\":21}");
        mapper.put("assistant_api_key", "db-secret-key");

        AssistantProperties properties = new AssistantProperties();
        properties.setProvider("deepseek");
        properties.setBaseUrl("https://env.example.com/v1");
        properties.setModel("env-model");
        properties.setTemperature(0.7D);
        properties.setTopP(0.95D);
        properties.setConnectTimeoutMs(12000L);
        properties.setReadTimeoutMs(120000L);
        properties.setMaxRequestsPerMinute(12);
        properties.setApiKey("env-secret-key");

        AssistantRuntimeConfigService service = new AssistantRuntimeConfigService(
                objectMapper,
                mapper,
                new FixedSiteConfigService(true),
                properties,
                null,
                null
        );

        AssistantRuntimeStatusDTO status = service.getRuntimeStatus();

        assertTrue(status.isEnabled());
        assertEquals("qwen", status.getProvider());
        assertEquals("Qwen", status.getProviderLabel());
        assertEquals("https://db.example.com/v1", status.getBaseUrl());
        assertEquals("db-model", status.getModel());
        assertEquals(0.4D, status.getTemperature());
        assertEquals(0.8D, status.getTopP());
        assertEquals(4096, status.getMaxTokens());
        assertEquals(9000L, status.getConnectTimeoutMs());
        assertEquals(60000L, status.getReadTimeoutMs());
        assertEquals(21, status.getMaxRequestsPerMinute());
        assertEquals("admin", status.getApiKeySource());
        assertEquals("admin", status.getRuntimeSource());
        assertTrue(status.isApiKeyConfigured());
    }

    @Test
    void shouldRejectCustomProviderWithoutBaseUrl() {
        AssistantProperties properties = new AssistantProperties();
        AssistantRuntimeConfigService service = new AssistantRuntimeConfigService(
                objectMapper,
                new InMemorySiteSettingMapper(),
                new FixedSiteConfigService(true),
                properties,
                null,
                null
        );

        AssistantRuntimeConfigUpdateRequest request = new AssistantRuntimeConfigUpdateRequest();
        request.setProvider("custom");
        request.setBaseUrl("");
        request.setModel("anything");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.saveRuntimeConfig(request));
        assertEquals("自定义供应商必须填写 Base URL", error.getMessage());
    }

    @Test
    void shouldFallbackToEnvApiKeyWhenNoStoredKeyExists() {
        AssistantProperties properties = new AssistantProperties();
        properties.setProvider("deepseek");
        properties.setModel("deepseek-v4-pro");
        properties.setApiKey("env-secret-key");

        AssistantRuntimeConfigService service = new AssistantRuntimeConfigService(
                objectMapper,
                new InMemorySiteSettingMapper(),
                new FixedSiteConfigService(false),
                properties,
                null,
                null
        );

        AssistantRuntimeStatusDTO status = service.getRuntimeStatus();

        assertFalse(status.isEnabled());
        assertTrue(status.isApiKeyConfigured());
        assertEquals("env", status.getApiKeySource());
        assertEquals("default", status.getRuntimeSource());
        assertEquals("env-********-key", status.getApiKeyMasked());
    }

    private static class InMemorySiteSettingMapper implements SiteSettingMapper {

        private final Map<String, SiteSetting> store = new HashMap<>();

        @Override
        public SiteSetting getByKey(String settingKey) {
            return store.get(settingKey);
        }

        @Override
        public void upsert(SiteSetting siteSetting) {
            store.put(siteSetting.getSettingKey(), siteSetting);
        }

        void put(String key, String value) {
            SiteSetting setting = new SiteSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            store.put(key, setting);
        }
    }

    private static class FixedSiteConfigService implements SiteConfigService {

        private final SiteConfigDTO config;

        private FixedSiteConfigService(boolean enabled) {
            this.config = SiteConfigDTO.emptyConfig();
            this.config.getAssistant().setEnabled(enabled);
        }

        @Override
        public SiteConfigDTO getConfig() {
            return config;
        }

        @Override
        public void saveConfig(SiteConfigDTO config, boolean preserveLegacyAssistantPrompt) {
        }
    }
}
