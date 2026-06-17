package cn.godlei.blogpojo.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章详情响应对象。
 * 在 {@code Post} 基础字段之上，额外返回结构化的分类路径、标签数组与相邻文章，
 * 供前台文章详情页直接消费（不再依赖前端解析 front-matter）。
 */
@Data
public class PostDetailVO {

    /** 文章 ID（短链 abbrlink） */
    private String id;

    /** 文章标题 */
    private String title;

    /** 文章正文（Markdown，含 front-matter） */
    private String content;

    /** 封面图片 URL */
    private String cover;

    /** 文章描述 */
    private String description;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 分类路径（从父到子），无分类时为空数组 */
    private List<String> categories = new ArrayList<>();

    /** 标签名数组，无标签时为空数组 */
    private List<String> tags = new ArrayList<>();

    /** 上一篇（更早发布），不存在则为 null */
    private PostNeighbor prev;

    /** 下一篇（更晚发布），不存在则为 null */
    private PostNeighbor next;
}
