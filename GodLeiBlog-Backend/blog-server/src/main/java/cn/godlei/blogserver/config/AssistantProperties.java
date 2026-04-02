package cn.godlei.blogserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.assistant")
public class AssistantProperties {

    private boolean enabled = true;

    private String baseUrl = "https://api.minimaxi.com/v1";

    private String apiKey = "";

    private String model = "MiniMax-M2.7";

    private long connectTimeoutMs = 10000L;

    private long readTimeoutMs = 120000L;

    private int maxRequestsPerMinute = 12;

    private int maxInputChars = 16000;
}
