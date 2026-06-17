package cn.godlei.blogserver.service.assistant;

import cn.godlei.blogpojo.dto.assistant.AssistantChatRequest;
import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeStatusDTO;

import java.io.IOException;
import java.io.OutputStream;

public interface AssistantService {

    void streamChat(AssistantChatRequest request, String clientKey, OutputStream outputStream) throws IOException;

    AssistantRuntimeStatusDTO getRuntimeStatus();

    AssistantRuntimeStatusDTO updateApiKey(String apiKey, boolean clearExisting);
}
