package cn.godlei.blogserver.service.site.storage;

import cn.godlei.blogserver.config.BlogStorageProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;

/**
 * 腾讯云 COS 对象存储。COSClient 懒加载（仅 mode=cos 真正使用时才初始化）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CosStorageProvider implements StorageProvider {

    private final BlogStorageProperties storageProperties;

    private volatile COSClient cosClient;

    @Override
    public String getType() {
        return "cos";
    }

    @Override
    public String store(byte[] content, String relativePath, String contentType) {
        BlogStorageProperties.Cos cos = storageProperties.getCos();

        String key = relativePath.replace("\\", "/");
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        // 共享桶内用 prefix 为本项目命名空间，如 wolfbook/post-cover/...
        String prefix = normalizeText(cos.getPrefix());
        if (StringUtils.hasText(prefix)) {
            key = trimSlashes(prefix) + "/" + key;
        }
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }

        PutObjectRequest request = new PutObjectRequest(
                cos.getBucket(), key, new ByteArrayInputStream(content), metadata);
        try {
            getClient().putObject(request);
        } catch (Exception ex) {
            throw new IllegalStateException("上传至 COS 失败", ex);
        }
        return buildAccessUrl(key);
    }

    private String buildAccessUrl(String key) {
        BlogStorageProperties.Cos cos = storageProperties.getCos();
        String baseUrl = normalizeText(cos.getPublicBaseUrl());
        if (StringUtils.hasText(baseUrl)) {
            return trimSlashes(baseUrl) + "/" + key;
        }
        return String.format("https://%s.cos.%s.myqcloud.com/%s", cos.getBucket(), cos.getRegion(), key);
    }

    private COSClient getClient() {
        COSClient client = this.cosClient;
        if (client == null) {
            synchronized (this) {
                client = this.cosClient;
                if (client == null) {
                    client = createClient();
                    this.cosClient = client;
                }
            }
        }
        return client;
    }

    private COSClient createClient() {
        BlogStorageProperties.Cos cos = storageProperties.getCos();
        if (!StringUtils.hasText(cos.getSecretId()) || !StringUtils.hasText(cos.getSecretKey())
                || !StringUtils.hasText(cos.getRegion()) || !StringUtils.hasText(cos.getBucket())) {
            throw new IllegalStateException(
                    "COS 配置不完整，请设置 upload.cos 的 secret-id/secret-key/region/bucket");
        }
        COSCredentials credentials = new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cos.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        log.info("初始化 COS 客户端，region={}, bucket={}", cos.getRegion(), cos.getBucket());
        return new COSClient(credentials, clientConfig);
    }

    @PreDestroy
    public void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    private String trimSlashes(String value) {
        String normalized = value;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
