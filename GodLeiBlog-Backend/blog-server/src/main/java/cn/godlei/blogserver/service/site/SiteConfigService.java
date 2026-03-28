package cn.godlei.blogserver.service.site;

import cn.godlei.blogpojo.dto.site.SiteConfigDTO;

public interface SiteConfigService {

    SiteConfigDTO getConfig();

    void saveConfig(SiteConfigDTO config);
}
