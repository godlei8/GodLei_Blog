package cn.godlei.blogpojo.dto.response.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantRuntimeConfigDTO {

    private String provider;

    private String baseUrl;

    private String model;

    private Double temperature;

    private Double topP;

    private Integer maxTokens;

    private Long connectTimeoutMs;

    private Long readTimeoutMs;

    private Integer maxRequestsPerMinute;
}
