package cn.godlei.blogserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MomentSchemaInitializer implements ApplicationRunner {

    private static final String CREATE_BLOG_MOMENT_TABLE = """
            CREATE TABLE IF NOT EXISTS blog_moment (
                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                content TEXT NOT NULL,
                location VARCHAR(255) NULL,
                publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                status TINYINT NOT NULL DEFAULT 0,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                KEY idx_blog_moment_status_publish_time (status, publish_time, id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_BLOG_MOMENT_MEDIA_TABLE = """
            CREATE TABLE IF NOT EXISTS blog_moment_media (
                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                moment_id BIGINT UNSIGNED NOT NULL,
                media_url VARCHAR(512) NOT NULL,
                sort INT NOT NULL DEFAULT 0,
                KEY idx_blog_moment_media_moment_sort (moment_id, sort, id),
                CONSTRAINT fk_blog_moment_media_moment
                    FOREIGN KEY (moment_id) REFERENCES blog_moment (id)
                    ON UPDATE CASCADE ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(CREATE_BLOG_MOMENT_TABLE);
            jdbcTemplate.execute(CREATE_BLOG_MOMENT_MEDIA_TABLE);
            log.info("Moments schema verified.");
        } catch (Exception ex) {
            log.error("Failed to verify moments schema.", ex);
            throw ex;
        }
    }
}
