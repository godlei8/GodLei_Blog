package cn.godlei.blogserver.controller.user;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogserver.service.site.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/site")
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    @GetMapping("/config")
    public Result getConfig() {
        return Result.success(siteConfigService.getConfig());
    }
}
