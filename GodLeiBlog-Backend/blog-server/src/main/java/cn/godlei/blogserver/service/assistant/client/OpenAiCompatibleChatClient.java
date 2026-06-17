package cn.godlei.blogserver.service.assistant.client;

import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeTestResultDTO;
import cn.godlei.blogserver.service.assistant.config.AssistantResolvedRuntimeConfig;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface OpenAiCompatibleChatClient {

    void streamChat(AssistantResolvedRuntimeConfig runtimeConfig,
                    List<Map<String, Object>> messages,
                    Consumer<String> onDelta);

    AssistantRuntimeTestResultDTO testConnection(AssistantResolvedRuntimeConfig runtimeConfig,
                                                 String systemPrompt);
}
