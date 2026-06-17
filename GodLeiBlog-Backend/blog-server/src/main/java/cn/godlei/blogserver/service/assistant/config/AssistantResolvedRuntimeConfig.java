package cn.godlei.blogserver.service.assistant.config;

import lombok.Data;

@Data
public class AssistantResolvedRuntimeConfig {

    private boolean enabled;

    private String provider;

    private String providerLabel;

    private String baseUrl;

    private String model;

    private String apiKey;

    private String apiKeySource;

    private String runtimeSource;

    private Double temperature;

    private Double topP;

    private Integer maxTokens;

    private long connectTimeoutMs;

    private long readTimeoutMs;

    private int maxRequestsPerMinute;

    private int maxInputChars;
}
