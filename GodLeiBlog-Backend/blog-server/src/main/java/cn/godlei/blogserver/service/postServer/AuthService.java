package cn.godlei.blogserver.service.postServer;

import cn.godlei.blogpojo.bo.Auth;
import cn.godlei.blogpojo.dto.response.LoginInfo;

public interface AuthService {


    /**
     * 管理员登录
     *
     * @param auth 登录参数
     * @return 登录成功后返回登录信息，失败返回 null
     */
    LoginInfo login(Auth auth);
}
