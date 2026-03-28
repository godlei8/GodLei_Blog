package cn.godlei.blogpojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteSetting {

    private String settingKey;

    private String settingValue;

    private String settingDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
