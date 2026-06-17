package cn.godlei.blogserver.controller.admin;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogpojo.dto.assistant.AssistantCredentialUpdateRequest;
import cn.godlei.blogpojo.dto.assistant.AssistantExperienceConfigUpdateRequest;
import cn.godlei.blogpojo.dto.assistant.AssistantRuntimeConfigUpdateRequest;
import cn.godlei.blogserver.service.assistant.config.AssistantExperienceConfigService;
import cn.godlei.blogserver.service.assistant.config.AssistantRuntimeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/assistant")
public class AdminAssistantController {

    private final AssistantRuntimeConfigService assistantRuntimeConfigService;
    private final AssistantExperienceConfigService assistantExperienceConfigService;

    @GetMapping("/runtime")
    public Result runtime() {
        return Result.success(assistantRuntimeConfigService.getRuntimeStatus());
    }

    @PutMapping("/runtime")
    public Result updateRuntime(@RequestBody(required = false) AssistantRuntimeConfigUpdateRequest request) {
        return Result.success(assistantRuntimeConfigService.saveRuntimeConfig(request));
    }

    @PostMapping("/runtime/test")
    public Result testRuntime(@RequestBody(required = false) AssistantRuntimeConfigUpdateRequest request) {
        return Result.success(assistantRuntimeConfigService.testRuntime(request));
    }

    @GetMapping("/experience")
    public Result experience() {
        return Result.success(assistantExperienceConfigService.getExperienceConfig());
    }

    @PutMapping("/experience")
    public Result updateExperience(@RequestBody(required = false) AssistantExperienceConfigUpdateRequest request) {
        return Result.success(assistantExperienceConfigService.saveExperienceConfig(request));
    }

    @GetMapping("/status")
    public Result status() {
        return Result.success(assistantRuntimeConfigService.getRuntimeStatus());
    }

    @PutMapping("/api-key")
    public Result updateApiKey(@RequestBody(required = false) AssistantCredentialUpdateRequest request) {
        return Result.success(assistantRuntimeConfigService.updateApiKey(request));
    }
}
