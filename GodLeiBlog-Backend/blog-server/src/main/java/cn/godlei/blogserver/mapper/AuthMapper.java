package cn.godlei.blogserver.mapper;

import cn.godlei.blogpojo.bo.Auth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

    cn.godlei.blogpojo.po.Auth findByUsername(@Param("username") String username);

    int countAdminUsers();

    int countUsersByUsername(@Param("username") String username);

    int insertDefaultAdmin(Auth auth);
}
