package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.entity.MediaFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MediaFileMapper {

    void insert(MediaFile mediaFile);
}
