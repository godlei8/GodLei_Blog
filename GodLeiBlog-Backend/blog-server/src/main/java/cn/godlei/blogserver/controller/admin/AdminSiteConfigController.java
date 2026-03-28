package cn.godlei.blogserver.controller.admin;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogpojo.dto.site.SiteConfigDTO;
import cn.godlei.blogserver.service.site.SiteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/site-config")
public class AdminSiteConfigController {

    private final SiteConfigService siteConfigService;

    @GetMapping("/get")
    public Result get() {
        return Result.success(siteConfigService.getConfig());
    }

    @PutMapping("/save")
    public Result save(@RequestBody SiteConfigDTO config) {
        log.info("保存站点配置");
        siteConfigService.saveConfig(config);
        return Result.success();
    }
}
