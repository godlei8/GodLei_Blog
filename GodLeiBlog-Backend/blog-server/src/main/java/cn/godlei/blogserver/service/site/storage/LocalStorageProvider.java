package cn.godlei.blogserver.service.site.storage;

import cn.godlei.blogserver.config.BlogStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储：写入 {@code upload.dir}，通过静态资源映射（/uploads，运行期经 /api 反代）对外访问。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStorageProvider implements StorageProvider {

    /** 本地访问路径前缀（运行期通过 /api 反代到后端静态资源） */
    public static final String PUBLIC_PATH = "/uploads";

    private final BlogStorageProperties storageProperties;

    @Override
    public String getType() {
        return "local";
    }

    @Override
    public String store(byte[] content, String relativePath, String contentType) {
        Path baseDir = Paths.get(storageProperties.getDir()).toAbsolutePath().normalize();
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
        String normalizedRelativePath = relativePath.replace("\\", "/");
        return "/api" + PUBLIC_PATH + "/" + normalizedRelativePath;
    }
}
