package cn.godlei.blogserver.service.site;

import cn.godlei.blogpojo.dto.site.SiteConfigDTO;

public interface SiteConfigService {

    SiteConfigDTO getConfig();

    default void saveConfig(SiteConfigDTO config) {
        saveConfig(config, true);
    }

    void saveConfig(SiteConfigDTO config, boolean preserveLegacyAssistantPrompt);
}
