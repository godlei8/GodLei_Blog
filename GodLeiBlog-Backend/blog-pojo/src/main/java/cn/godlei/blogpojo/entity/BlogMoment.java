package cn.godlei.blogpojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogMoment {

    private Long id;

    private String content;

    private String location;

    private LocalDateTime publishTime;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
