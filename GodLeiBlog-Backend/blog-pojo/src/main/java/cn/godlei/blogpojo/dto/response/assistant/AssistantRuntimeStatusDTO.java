package cn.godlei.blogpojo.dto.response.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantRuntimeStatusDTO {

    private boolean enabled;

    private String model;

    private boolean apiKeyConfigured;

    private String apiKeyMasked;

    private String apiKeySource;

    private String baseUrl;

    private int maxRequestsPerMinute;
}
