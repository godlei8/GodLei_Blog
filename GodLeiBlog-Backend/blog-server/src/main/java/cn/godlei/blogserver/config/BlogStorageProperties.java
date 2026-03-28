package cn.godlei.blogserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.storage")
public class BlogStorageProperties {

    private String mode = "local";

    private String publicBaseUrl = "";

    private Local local = new Local();

    @Data
    public static class Local {

        private String baseDir = "uploads";

        private String publicPath = "/uploads";
    }
}
