package cn.godlei.blogserver.service.site.storage;

import cn.godlei.blogserver.config.BlogStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储：写入 {@code blog.storage.local.base-dir}，通过静态资源映射对外访问。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStorageProvider implements StorageProvider {

    private final BlogStorageProperties storageProperties;

    @Override
    public String getType() {
        return "local";
    }

    @Override
    public String store(byte[] content, String relativePath, String contentType) {
        Path baseDir = Paths.get(storageProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        Path target = baseDir.resolve(relativePath).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("非法的存储路径");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new IllegalStateException("保存上传文件失败", ex);
        }
        return buildAccessUrl(relativePath);
    }

    private String buildAccessUrl(String relativePath) {
        String publicPath = normalizePublicPath(storageProperties.getLocal().getPublicPath());
        String normalizedRelativePath = relativePath.replace("\\", "/");

        String publicBaseUrl = normalizeText(storageProperties.getPublicBaseUrl());
        if (StringUtils.hasText(publicBaseUrl)) {
            return trimTrailingSlash(publicBaseUrl) + publicPath + "/" + normalizedRelativePath;
        }
        // 运行期通过 /api 前缀反代到后端静态资源
        String runtimePath = publicPath.startsWith("/api/") ? publicPath : "/api" + publicPath;
        return runtimePath + "/" + normalizedRelativePath;
    }

    private String normalizePublicPath(String path) {
        if (!StringUtils.hasText(path)) {
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

    private String trimTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
