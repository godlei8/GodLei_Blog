package cn.godlei.blogserver.service.assistant.config;

import cn.godlei.blogpojo.dto.response.assistant.AssistantExperienceConfigDTO;
import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogpojo.entity.SiteSetting;
import cn.godlei.blogserver.mapper.SiteSettingMapper;
import cn.godlei.blogserver.service.site.SiteConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssistantExperienceConfigServiceTest {

    @Test
    void shouldReadLegacySystemPromptWhenPrivatePromptConfigIsEmpty() {
        InMemorySiteSettingMapper mapper = new InMemorySiteSettingMapper();
        mapper.put("site_config", "{\"assistant\":{\"enabled\":true,\"name\":\"馨宝\",\"subtitle\":\"站内 AI 助手\",\"welcomeMessage\":\"hello\",\"starterPrompts\":[\"A\"],\"disclaimer\":\"tip\",\"systemPrompt\":\"legacy prompt\"}}");

        AssistantExperienceConfigService service = new AssistantExperienceConfigService(
                new FixedSiteConfigService(),
                mapper,
                new ObjectMapper()
        );

        AssistantExperienceConfigDTO result = service.getExperienceConfig();

        assertEquals("legacy prompt", result.getSystemPrompt());
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

        private final SiteConfigDTO config = SiteConfigDTO.emptyConfig();

        @Override
        public SiteConfigDTO getConfig() {
            return config;
        }

        @Override
        public void saveConfig(SiteConfigDTO config, boolean preserveLegacyAssistantPrompt) {
        }
    }
}
