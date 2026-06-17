package cn.godlei.blogserver.config;

import cn.godlei.blogpojo.entity.MediaFile;
import cn.godlei.blogserver.mapper.MediaFileMapper;
import cn.godlei.blogserver.mapper.PostMapper;
import cn.godlei.blogserver.mapper.SiteSettingMapper;
import cn.godlei.blogserver.service.site.storage.CosStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 一次性迁移工具：把本地 uploads 目录里的历史图片上传到腾讯云 COS，
 * 并把 blog_media_file / blog_post.cover / blog_site_setting 里的旧 URL 精确替换为 COS URL。
 *
 * <p>默认不启用。迁移时设置并启动一次：
 * <pre>
 *   upload.migrate-local-to-cos=true （或环境变量 UPLOAD_MIGRATE_LOCAL_TO_COS=true）
 *   upload.cos.* 凭据（secret-id/secret-key/region/bucket）须已配置
 * </pre>
 * 迁移完成、核对无误后，再把该开关关掉并将 upload.provider 切到 cos。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "upload.migrate-local-to-cos", havingValue = "true")
public class StorageMigrationRunner implements CommandLineRunner {

    private final BlogStorageProperties storageProperties;
    private final MediaFileMapper mediaFileMapper;
    private final PostMapper postMapper;
    private final SiteSettingMapper siteSettingMapper;
    private final CosStorageProvider cosStorageProvider;

    @Override
    public void run(String... args) {
        List<MediaFile> localFiles = mediaFileMapper.findByStorageType("local");
        log.info("[存储迁移] 开始，将 {} 个本地文件迁移到 COS", localFiles.size());

        Path baseDir = Paths.get(storageProperties.getDir()).toAbsolutePath().normalize();
        int ok = 0;
        int skipped = 0;
        int failed = 0;

        for (MediaFile media : localFiles) {
            String relativePath = media.getRelativePath();
            Path localPath = baseDir.resolve(relativePath).normalize();
            try {
                if (!Files.exists(localPath)) {
                    log.warn("[存储迁移] 跳过：本地文件不存在 id={}, path={}", media.getId(), localPath);
                    skipped++;
                    continue;
                }
                byte[] content = Files.readAllBytes(localPath);
                String oldUrl = media.getAccessUrl();
                String newUrl = cosStorageProvider.store(content, relativePath, media.getContentType());

                mediaFileMapper.updateLocation(media.getId(), newUrl, "cos");
                int coverUpdated = postMapper.updateCoverUrl(oldUrl, newUrl);
                int settingUpdated = siteSettingMapper.replaceUrlInValues(oldUrl, newUrl);

                log.info("[存储迁移] OK id={} | {} -> {} | 封面更新 {} 行, 站点配置更新 {} 行",
                        media.getId(), oldUrl, newUrl, coverUpdated, settingUpdated);
                ok++;
            } catch (Exception ex) {
                log.error("[存储迁移] 失败 id={}, path={}: {}", media.getId(), relativePath, ex.getMessage(), ex);
                failed++;
            }
        }

        log.info("[存储迁移] 完成：成功 {}，跳过 {}，失败 {}。", ok, skipped, failed);
        log.warn("[存储迁移] 请人工核对前台展示无误后：1) 关闭 upload.migrate-local-to-cos；"
                + "2) 将 upload.provider 切为 cos；3) 从仓库移除本地 uploads 目录。");
    }
}
