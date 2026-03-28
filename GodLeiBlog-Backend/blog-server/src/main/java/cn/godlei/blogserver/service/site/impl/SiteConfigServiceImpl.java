package cn.godlei.blogserver.service.site.impl;

import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogpojo.entity.SiteSetting;
import cn.godlei.blogserver.mapper.SiteSettingMapper;
import cn.godlei.blogserver.service.site.SiteConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteConfigServiceImpl implements SiteConfigService {

    private static final String SITE_CONFIG_KEY = "site_config";

    private final SiteSettingMapper siteSettingMapper;

    private final ObjectMapper objectMapper;

    @Override
    public SiteConfigDTO getConfig() {
        SiteSetting setting = siteSettingMapper.getByKey(SITE_CONFIG_KEY);
        if (setting == null || !StringUtils.hasText(setting.getSettingValue())) {
            return SiteConfigDTO.emptyConfig();
        }
        try {
            SiteConfigDTO config = objectMapper.readValue(setting.getSettingValue(), SiteConfigDTO.class);
            if (config == null) {
                return SiteConfigDTO.emptyConfig();
            }
            config.normalize();
            return config;
        } catch (Exception ex) {
            log.warn("解析站点配置失败，返回默认配置", ex);
            return SiteConfigDTO.emptyConfig();
        }
    }

    @Override
    public void saveConfig(SiteConfigDTO config) {
        SiteConfigDTO safeConfig = config == null ? SiteConfigDTO.emptyConfig() : config;
        safeConfig.normalize();

        SiteSetting siteSetting = new SiteSetting();
        siteSetting.setSettingKey(SITE_CONFIG_KEY);
        siteSetting.setSettingDesc("站点首页与关于页配置");
        siteSetting.setSettingValue(writeConfig(safeConfig));

        siteSettingMapper.upsert(siteSetting);
    }

    private String writeConfig(SiteConfigDTO config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化站点配置失败", ex);
        }
    }
}
