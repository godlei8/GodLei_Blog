package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.entity.BlogMomentMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogMomentMediaMapper {

    List<BlogMomentMedia> listByMomentIds(@Param("momentIds") List<Long> momentIds);

    void deleteByMomentId(@Param("momentId") Long momentId);

    void batchInsert(@Param("mediaList") List<BlogMomentMedia> mediaList);
}
