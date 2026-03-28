package cn.godlei.blogserver.service.postServer.Impl;

import cn.godlei.blogcommon.exception.AuthenticationException;
import cn.godlei.blogcommon.util.JwtUtil;
import cn.godlei.blogcommon.util.PasswordMatcher;
import cn.godlei.blogpojo.bo.Auth;
import cn.godlei.blogpojo.dto.response.LoginInfo;
import cn.godlei.blogserver.mapper.AuthMapper;
import cn.godlei.blogserver.service.postServer.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AuthServerImpl implements AuthService {

    @Autowired
    private AuthMapper authMapper;

    @Override
    public LoginInfo login(Auth auth) {
        Auth sanitized = sanitize(auth);
        if (sanitized == null) {
            throw new AuthenticationException(400, "用户名或密码不能为空");
        }

        cn.godlei.blogpojo.po.Auth matched = authMapper.findByUsername(sanitized.getUsername());
        if (matched == null) {
            matched = tryBootstrapDefaultAdmin(sanitized);
        }

        if (matched == null) {
            log.warn("登录失败，账号不存在：{}", sanitized.getUsername());
            throw new AuthenticationException(403, "账号不存在或密码错误");
        }

        if (!isAdmin(matched)) {
            log.warn("登录失败，账号没有后台权限：{}", sanitized.getUsername());
            throw new AuthenticationException(403, "当前账号没有后台权限");
        }

        if (!isEnabled(matched)) {
            log.warn("登录失败，账号已被禁用：{}", sanitized.getUsername());
            throw new AuthenticationException(403, "当前账号已被禁用");
        }

        if (!PasswordMatcher.matches(sanitized.getPassword(), matched.getPassword())) {
            log.warn("登录失败，密码不匹配：{}", sanitized.getUsername());
            throw new AuthenticationException(403, "账号不存在或密码错误");
        }

        log.info("登录成功，账号：{}", matched.getUsername());
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", matched.getUsername());
        String jwt = JwtUtil.generateToken(claims);
        return new LoginInfo(matched.getId(), matched.getUsername(), matched.getNickname(), jwt);
    }

    private Auth sanitize(Auth auth) {
        if (auth == null || !StringUtils.hasText(auth.getUsername()) || !StringUtils.hasText(auth.getPassword())) {
            return null;
        }

        Auth sanitized = new Auth();
        sanitized.setUsername(auth.getUsername().trim());
        sanitized.setPassword(auth.getPassword());
        return sanitized;
    }

    private cn.godlei.blogpojo.po.Auth tryBootstrapDefaultAdmin(Auth auth) {
        if (!"admin".equals(auth.getUsername()) || !"admin".equals(auth.getPassword())) {
            return null;
        }
        if (authMapper.countAdminUsers() > 0 || authMapper.countUsersByUsername("admin") > 0) {
            return null;
        }

        Auth defaultAdmin = new Auth();
        defaultAdmin.setUsername("admin");
        defaultAdmin.setPassword("admin");
        authMapper.insertDefaultAdmin(defaultAdmin);
        log.warn("未检测到管理员账号，已自动创建默认账号 admin/admin");
        return authMapper.findByUsername(defaultAdmin.getUsername());
    }

    private boolean isAdmin(cn.godlei.blogpojo.po.Auth auth) {
        return auth != null && Integer.valueOf(1).equals(auth.getRole());
    }

    private boolean isEnabled(cn.godlei.blogpojo.po.Auth auth) {
        return auth != null && (auth.getStatus() == null || Integer.valueOf(1).equals(auth.getStatus()));
    }
}
