package cn.godlei.blogpojo.dto.assistant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssistantRuntimeConfigUpdateRequest {

    private String provider;

    private String baseUrl;

    private String model;

    private Double temperature;

    private Double topP;

    private Integer maxTokens;

    private Long connectTimeoutMs;

    private Long readTimeoutMs;

    private Integer maxRequestsPerMinute;

    private String apiKey;

    private boolean clearApiKey;
}
