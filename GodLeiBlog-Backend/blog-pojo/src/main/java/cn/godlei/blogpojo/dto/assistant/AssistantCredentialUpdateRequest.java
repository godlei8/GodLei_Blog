package cn.godlei.blogpojo.dto.assistant;

import lombok.Data;

@Data
public class AssistantCredentialUpdateRequest {

    private String apiKey;

    private boolean clearExisting;
}
