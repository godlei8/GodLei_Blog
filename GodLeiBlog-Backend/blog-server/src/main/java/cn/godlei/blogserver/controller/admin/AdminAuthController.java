package cn.godlei.blogserver.controller.admin;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogpojo.bo.Auth;
import cn.godlei.blogpojo.dto.response.LoginInfo;
import cn.godlei.blogserver.service.postServer.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @Autowired
    private AuthService authServer;

    @PostMapping("/login")
    public Result<LoginInfo> login(@RequestBody Auth auth) {
        String username = auth == null ? "" : auth.getUsername();
        if (!StringUtils.hasText(username)) {
            log.info("管理员登录尝试，账号为空");
        } else {
            log.info("管理员登录尝试，账号：{}", username);
        }
        return Result.success(authServer.login(auth));
    }
}
