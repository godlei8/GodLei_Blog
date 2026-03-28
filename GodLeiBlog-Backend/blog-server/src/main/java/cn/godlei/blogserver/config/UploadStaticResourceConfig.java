package cn.godlei.blogserver.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class UploadStaticResourceConfig implements WebMvcConfigurer {

    private final BlogStorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicPath = normalizePublicPath(storageProperties.getLocal().getPublicPath());
        String apiPublicPath = publicPath.startsWith("/api/") ? publicPath : "/api" + publicPath;
        Path baseDir = Paths.get(storageProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        String location = baseDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(publicPath + "/**", apiPublicPath + "/**")
                .addResourceLocations(location);
    }

    private String normalizePublicPath(String path) {
        if (path == null || path.isBlank()) {
            return "/uploads";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
