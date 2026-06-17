package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.entity.SiteSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SiteSettingMapper {

    SiteSetting getByKey(@Param("settingKey") String settingKey);

    void upsert(SiteSetting siteSetting);

    /** 将所有站点配置值中出现的旧 URL 精确替换为新 URL（存储迁移用）。 */
    int replaceUrlInValues(@Param("oldUrl") String oldUrl, @Param("newUrl") String newUrl);
}
