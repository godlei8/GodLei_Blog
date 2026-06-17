package cn.godlei.blogserver.service.site.impl;

import cn.godlei.blogpojo.dto.response.MediaUploadResult;
import cn.godlei.blogpojo.entity.MediaFile;
import cn.godlei.blogserver.config.BlogStorageProperties;
import cn.godlei.blogserver.mapper.MediaFileMapper;
import cn.godlei.blogserver.service.site.MediaService;
import cn.godlei.blogserver.service.site.storage.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MediaServiceImpl implements MediaService {

    private final BlogStorageProperties storageProperties;
    private final MediaFileMapper mediaFileMapper;
    private final Map<String, StorageProvider> providers;

    public MediaServiceImpl(BlogStorageProperties storageProperties,
                            MediaFileMapper mediaFileMapper,
                            List<StorageProvider> storageProviders) {
        this.storageProperties = storageProperties;
        this.mediaFileMapper = mediaFileMapper;
        this.providers = storageProviders.stream()
                .collect(Collectors.toMap(p -> p.getType().toLowerCase(Locale.ROOT), Function.identity()));
    }

    @Override
    public MediaUploadResult uploadImage(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 1) 大小上限（业务层校验，独立于 multipart 限制）
        long maxBytes = storageProperties.getMaxImageSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "图片大小超过限制（最大 " + storageProperties.getMaxImageSize().toMegabytes() + "MB）");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("读取上传文件失败", ex);
        }

        // 2) 按文件头魔数识别真实类型，仅放行 jpg/png/gif/webp（拒绝 SVG 等）
        ImageType detected = detectImageType(content);
        if (detected == null) {
            throw new IllegalArgumentException("仅支持 JPG/PNG/GIF/WEBP 图片，且文件内容须与格式一致");
        }

        // 3) 声明的 Content-Type 若与真实内容不符则拒绝
        String declaredContentType = normalizeText(file.getContentType()).toLowerCase(Locale.ROOT);
        if (StringUtils.hasText(declaredContentType)
                && !declaredContentType.startsWith("image/")) {
            throw new IllegalArgumentException("非法的 Content-Type：" + declaredContentType);
        }
        if (StringUtils.hasText(declaredContentType)
                && !detected.matchesContentType(declaredContentType)) {
            throw new IllegalArgumentException("文件内容与声明的类型不一致");
        }

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String safeBizType = normalizeBizType(bizType);
        String storedName = UUID.randomUUID().toString().replace("-", "") + detected.extension;
        LocalDate now = LocalDate.now();
        String relativePath = String.format("%s/%04d/%02d/%02d/%s",
                safeBizType, now.getYear(), now.getMonthValue(), now.getDayOfMonth(), storedName);

        // 4) 选择存储 provider 并保存
        StorageProvider provider = resolveProvider();
        String accessUrl = provider.store(content, relativePath, detected.contentType);

        // 5) 记录 media_file
        MediaFile mediaFile = new MediaFile();
        mediaFile.setBizType(safeBizType);
        mediaFile.setStorageType(provider.getType());
        mediaFile.setOriginalName(originalName);
        mediaFile.setStoredName(storedName);
        mediaFile.setExtension(detected.extension);
        mediaFile.setContentType(detected.contentType);
        mediaFile.setFileSize((long) content.length);
        mediaFile.setRelativePath(relativePath);
        mediaFile.setAccessUrl(accessUrl);
        mediaFileMapper.insert(mediaFile);

        log.info("图片上传成功，storage={}, bizType={}, path={}", provider.getType(), safeBizType, relativePath);

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

    private StorageProvider resolveProvider() {
        String mode = normalizeText(storageProperties.getMode()).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(mode)) {
            mode = "local";
        }
        StorageProvider provider = providers.get(mode);
        if (provider == null) {
            throw new IllegalStateException("不支持的存储模式：" + mode + "，可选 " + providers.keySet());
        }
        return provider;
    }

    /** 受支持的图片类型及其魔数判定。 */
    private enum ImageType {
        JPEG(".jpg", "image/jpeg"),
        PNG(".png", "image/png"),
        GIF(".gif", "image/gif"),
        WEBP(".webp", "image/webp");

        final String extension;
        final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        boolean matchesContentType(String declared) {
            if (this == JPEG) {
                return declared.equals("image/jpeg") || declared.equals("image/jpg");
            }
            return declared.equals(contentType);
        }
    }

    private ImageType detectImageType(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return ImageType.JPEG;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return ImageType.PNG;
        }
        // GIF: "GIF8"
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return ImageType.GIF;
        }
        // WEBP: "RIFF"...."WEBP"
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return ImageType.WEBP;
        }
        return null;
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
