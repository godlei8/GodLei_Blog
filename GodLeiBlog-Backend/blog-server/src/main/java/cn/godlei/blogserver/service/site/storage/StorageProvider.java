package cn.godlei.blogserver.service.site.storage;

/**
 * 对象存储抽象：按 {@code upload.provider} 选择具体实现（local / cos）。
 */
public interface StorageProvider {

    /**
     * 存储类型标识，对应 {@code upload.provider}（如 "local"、"cos"）。
     */
    String getType();

    /**
     * 保存文件并返回可公网访问的 URL。
     *
     * @param content      文件字节内容
     * @param relativePath 存储相对路径（如 post-cover/2026/06/17/xxx.jpg），不以 / 开头
     * @param contentType  MIME 类型
     * @return 可访问 URL
     */
    String store(byte[] content, String relativePath, String contentType);
}
