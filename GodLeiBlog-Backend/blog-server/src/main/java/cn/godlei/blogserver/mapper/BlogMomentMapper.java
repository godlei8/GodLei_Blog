package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.dto.request.MomentQueryParam;
import cn.godlei.blogpojo.entity.BlogMoment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogMomentMapper {

    List<BlogMoment> listAdmin(MomentQueryParam queryParam);

    List<BlogMoment> listPublic(MomentQueryParam queryParam);

    BlogMoment getById(@Param("id") Long id);

    void insert(BlogMoment moment);

    void update(BlogMoment moment);

    void delete(@Param("id") Long id);
}
