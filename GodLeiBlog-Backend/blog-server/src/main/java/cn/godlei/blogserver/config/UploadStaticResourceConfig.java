package cn.godlei.blogserver.config;

import cn.godlei.blogserver.service.site.storage.LocalStorageProvider;
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
        String publicPath = LocalStorageProvider.PUBLIC_PATH;            // /uploads
        String apiPublicPath = "/api" + publicPath;                     // /api/uploads
        Path baseDir = Paths.get(storageProperties.getDir()).toAbsolutePath().normalize();
        String location = baseDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(publicPath + "/**", apiPublicPath + "/**")
                .addResourceLocations(location);
    }
}
