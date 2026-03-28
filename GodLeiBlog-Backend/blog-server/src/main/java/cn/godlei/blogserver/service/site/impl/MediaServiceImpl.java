package cn.godlei.blogserver.service.site.impl;

import cn.godlei.blogpojo.dto.response.MediaUploadResult;
import cn.godlei.blogpojo.entity.MediaFile;
import cn.godlei.blogserver.config.BlogStorageProperties;
import cn.godlei.blogserver.mapper.MediaFileMapper;
import cn.godlei.blogserver.service.site.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final BlogStorageProperties storageProperties;

    private final MediaFileMapper mediaFileMapper;

    @Override
    public MediaUploadResult uploadImage(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        if (!"local".equalsIgnoreCase(storageProperties.getMode())) {
            throw new IllegalStateException("当前仅实现 local 存储模式，请先将 blog.storage.mode 配置为 local");
        }

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String contentType = normalizeText(file.getContentType());
        if (!isImageFile(contentType, originalName)) {
            throw new IllegalArgumentException("仅支持上传图片文件");
        }

        String safeBizType = normalizeBizType(bizType);
        String extension = resolveExtension(originalName, contentType);
        LocalDate now = LocalDate.now();
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        String relativePath = String.format(
                "%s/%04d/%02d/%02d/%s",
                safeBizType,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                storedName
        );

        Path baseDir = Paths.get(storageProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        Path target = baseDir.resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("保存上传文件失败", ex);
        }

        String accessUrl = buildAccessUrl(relativePath);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setBizType(safeBizType);
        mediaFile.setStorageType("local");
        mediaFile.setOriginalName(originalName);
        mediaFile.setStoredName(storedName);
        mediaFile.setExtension(extension);
        mediaFile.setContentType(contentType);
        mediaFile.setFileSize(file.getSize());
        mediaFile.setRelativePath(relativePath);
        mediaFile.setAccessUrl(accessUrl);
        mediaFileMapper.insert(mediaFile);

        log.info("图片上传成功，bizType={}, path={}", safeBizType, relativePath);

        MediaUploadResult result = new MediaUploadResult();
        result.setId(mediaFile.getId());
        result.setBizType(mediaFile.getBizType());
        result.setStorageType(mediaFile.getStorageType());
        result.setOriginalName(mediaFile.getOriginalName());
        result.setContentType(mediaFile.getContentType());
        result.setFileSize(mediaFile.getFileSize());
        result.setUrl(mediaFile.getAccessUrl());
        return result;
    }

    private String buildAccessUrl(String relativePath) {
        String publicPath = normalizePublicPath(storageProperties.getLocal().getPublicPath());
        String normalizedRelativePath = relativePath.replace("\\", "/");
        String path = buildRuntimeAccessPath(publicPath, normalizedRelativePath);

        String publicBaseUrl = normalizeText(storageProperties.getPublicBaseUrl());
        if (!StringUtils.hasText(publicBaseUrl)) {
            return path;
        }
        return trimTrailingSlash(publicBaseUrl) + publicPath + "/" + normalizedRelativePath;
    }

    private String buildRuntimeAccessPath(String publicPath, String relativePath) {
        String runtimePath = publicPath.startsWith("/api/") ? publicPath : "/api" + publicPath;
        return runtimePath + "/" + relativePath;
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

    private String normalizeOriginalName(String filename) {
        String safe = normalizeText(filename);
        if (!StringUtils.hasText(safe)) {
            return "image";
        }
        String normalized = safe.replace("\\", "/");
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isImageFile(String contentType, String originalName) {
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String extension = resolveExtension(originalName, contentType);
        return extension.matches("\\.(jpg|jpeg|png|gif|webp|bmp|svg)$");
    }

    private String resolveExtension(String originalName, String contentType) {
        int index = originalName.lastIndexOf('.');
        if (index >= 0 && index < originalName.length() - 1) {
            return "." + originalName.substring(index + 1).toLowerCase(Locale.ROOT);
        }

        if (!StringUtils.hasText(contentType)) {
            return ".png";
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            case "image/svg+xml" -> ".svg";
            default -> ".png";
        };
    }

    private String normalizeBizType(String bizType) {
        String source = normalizeText(bizType).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(source)) {
            return "common";
        }
        String normalized = source.replaceAll("[^a-z0-9-]", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return StringUtils.hasText(normalized) ? normalized : "common";
    }
}
