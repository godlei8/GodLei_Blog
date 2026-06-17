package cn.godlei.blogpojo.entity;

import lombok.Data;

@Data
public class BlogMomentMedia {

    private Long id;

    private Long momentId;

    private String mediaUrl;

    private Integer sort;
}
