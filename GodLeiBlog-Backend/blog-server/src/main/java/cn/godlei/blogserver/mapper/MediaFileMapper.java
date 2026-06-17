package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.entity.MediaFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MediaFileMapper {

    void insert(MediaFile mediaFile);

    /** 按存储类型查询（迁移用）。 */
    List<MediaFile> findByStorageType(@Param("storageType") String storageType);

    /** 更新文件的访问地址与存储类型（迁移用）。 */
    int updateLocation(@Param("id") Long id,
                       @Param("accessUrl") String accessUrl,
                       @Param("storageType") String storageType);
}
