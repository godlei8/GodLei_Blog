package cn.godlei.blogpojo.dto.response;

import lombok.Data;

/**
 * 相邻文章（上一篇 / 下一篇）的精简信息。
 */
@Data
public class PostNeighbor {

    /** 文章 ID（短链 abbrlink） */
    private String id;

    /** 文章标题 */
    private String title;
}
