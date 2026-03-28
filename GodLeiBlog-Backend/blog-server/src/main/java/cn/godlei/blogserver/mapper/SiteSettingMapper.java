package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.entity.SiteSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SiteSettingMapper {

    SiteSetting getByKey(@Param("settingKey") String settingKey);

    void upsert(SiteSetting siteSetting);
}
