package cn.godlei.blogserver.controller.admin;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogpojo.dto.assistant.AssistantCredentialUpdateRequest;
import cn.godlei.blogserver.service.assistant.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/assistant")
public class AdminAssistantController {

    private final AssistantService assistantService;

    @GetMapping("/status")
    public Result status() {
        return Result.success(assistantService.getRuntimeStatus());
    }

    @PutMapping("/api-key")
    public Result updateApiKey(@RequestBody(required = false) AssistantCredentialUpdateRequest request) {
        String apiKey = request == null ? null : request.getApiKey();
        boolean clearExisting = request != null && request.isClearExisting();
        return Result.success(assistantService.updateApiKey(apiKey, clearExisting));
    }
}
