package cn.godlei.blogserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.assistant")
public class AssistantProperties {

    private boolean enabled = true;

    private String provider = "deepseek";

    private String baseUrl = "";

    private String apiKey = "";

    private String model = "deepseek-v4-pro";

    private Double temperature = 0.7D;

    private Double topP = 0.95D;

    private Integer maxTokens;

    private long connectTimeoutMs = 10000L;

    private long readTimeoutMs = 120000L;

    private int maxRequestsPerMinute = 12;

    private int maxInputChars = 16000;
}
