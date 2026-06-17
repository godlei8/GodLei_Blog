package cn.godlei.blogserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Data
@Component
@ConfigurationProperties(prefix = "blog.storage")
public class BlogStorageProperties {

    /** 存储模式：local（本地磁盘）/ cos（腾讯云对象存储） */
    private String mode = "local";

    /** 可选的公网访问前缀（CDN/自定义域名）；留空则用各 provider 的默认拼接 */
    private String publicBaseUrl = "";

    /** 单个图片大小上限（业务层校验，独立于 multipart 限制） */
    private DataSize maxImageSize = DataSize.ofMegabytes(5);

    private Local local = new Local();

    private Cos cos = new Cos();

    @Data
    public static class Local {

        private String baseDir = "uploads";

        private String publicPath = "/uploads";
    }

    @Data
    public static class Cos {

        /** 腾讯云访问密钥 ID（经环境变量注入，勿入库） */
        private String secretId = "";

        /** 腾讯云访问密钥 Key（经环境变量注入，勿入库） */
        private String secretKey = "";

        /** 存储桶所在地域，如 ap-guangzhou */
        private String region = "";

        /** 存储桶名称，如 blog-1250000000 */
        private String bucket = "";

        /** 可选的公网访问域名（CDN/自定义域名），留空则用 COS 默认域名 */
        private String baseUrl = "";
    }
}
