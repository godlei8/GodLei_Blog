package cn.godlei.blogpojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MediaFile {

    private Long id;

    private String bizType;

    private String storageType;

    private String originalName;

    private String storedName;

    private String extension;

    private String contentType;

    private Long fileSize;

    private String relativePath;

    private String accessUrl;

    private LocalDateTime createdAt;
}
