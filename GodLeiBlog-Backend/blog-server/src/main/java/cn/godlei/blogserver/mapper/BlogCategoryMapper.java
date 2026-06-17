package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.entity.BlogCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogCategoryMapper {

    BlogCategory findByParentAndName(@Param("parentId") Long parentId,
                                     @Param("name") String name);

    /**
     * 按 ID 查询分类（用于回溯分类层级路径）。
     */
    BlogCategory findById(@Param("id") Long id);

    void insert(BlogCategory category);

    List<BlogCategory> list(@Param("keyword") String keyword);

    void update(BlogCategory category);

    void deleteById(@Param("id") Long id);
}

