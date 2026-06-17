package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.dto.request.PostBody;
import cn.godlei.blogpojo.dto.request.PostQueryParam;
import cn.godlei.blogpojo.dto.response.PostNeighbor;
import cn.godlei.blogpojo.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章Mapper接口
 */
@Mapper
public interface PostMapper {

    /**
     * 分页查询文章
     */
    List<Post> pages(PostQueryParam queryParam);

    /**
     * 根据ID查询文章
     */
    Post get(String id);

    /**
     * 添加文章
     */
    void add(PostBody postBody);

    /**
     * 更新文章
     */
    void update(PostBody postBody);

    /**
     * 删除文章
     */
    void delete(String id);

    /**
     * 按分类ID查询文章简要列表
     */
    List<Post> listByCategoryId(Long categoryId);

    /**
     * 按标签ID查询文章简要列表
     */
    List<Post> listByTagId(Long tagId);

    /**
     * 查询某篇文章的标签名列表（JOIN blog_post_tag + blog_tag）。
     */
    List<String> listTagNamesByPostId(@Param("postId") String postId);

    /**
     * 上一篇：比给定发布时间更早、最接近的一篇。
     */
    PostNeighbor findPrevByCreateTime(@Param("createTime") LocalDateTime createTime);

    /**
     * 下一篇：比给定发布时间更晚、最接近的一篇。
     */
    PostNeighbor findNextByCreateTime(@Param("createTime") LocalDateTime createTime);
}
