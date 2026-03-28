package cn.godlei.blogpojo.dto.response;

import lombok.Data;

@Data
public class MediaUploadResult {

    private Long id;

    private String bizType;

    private String storageType;

    private String originalName;

    private String contentType;

    private Long fileSize;

    private String url;
}
