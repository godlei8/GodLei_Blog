package cn.godlei.blogserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 上传/存储配置，对应 yaml 前缀 {@code upload}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class BlogStorageProperties {

    /** 存储提供方：local（本地磁盘）/ cos（腾讯云对象存储） */
    private String provider = "local";

    /** 本地存储目录（provider=local 时使用） */
    private String dir = "./uploads";

    /** 单个图片大小上限（业务层校验，独立于 multipart 限制） */
    private DataSize maxImageSize = DataSize.ofMegabytes(5);

    private Cos cos = new Cos();

    @Data
    public static class Cos {

        /** 腾讯云访问密钥 ID（经本地/环境配置注入，勿入库） */
        private String secretId = "";

        /** 腾讯云访问密钥 Key（经本地/环境配置注入，勿入库） */
        private String secretKey = "";

        /** 存储桶名称，如 godlei-1311686380 */
        private String bucket = "";

        /** 存储桶所在地域，如 ap-chengdu */
        private String region = "";

        /** 公网访问域名（CDN/自定义域名），如 http://cos.godlei8.top */
        private String publicBaseUrl = "";

        /** 对象键前缀（用于在共享桶内为本项目命名空间），如 wolfbook */
        private String prefix = "";
    }
}
