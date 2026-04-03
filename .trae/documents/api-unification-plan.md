# API 接口统一规范与部署计划

## 目标

统一前后端接口规范，确保所有接口调用正常，然后重新部署到服务器。

## 现状分析

### 后端控制器路径（需要统一）

| 模块 | 控制器 | 当前路径 | 建议统一路径 |
|------|--------|----------|-------------|
| 站点配置 | SiteConfigController | `/site` | ✅ 保持不变 |
| 文章 | PostController | `/post` | ✅ 保持不变 |
| 统计 | SiteStatsController | `/post/stats` | ✅ 保持不变 |
| 日志 | LogsController | `/logs` | ✅ 保持不变 |
| 友链 | LinksController | `/links` | ✅ 保持不变 |
| 动态 | MomentsController | `/moments` | ✅ 保持不变 |
| AI助手 | AssistantController | `/assistant` | ✅ 保持不变 |
| 管理端认证 | AdminAuthController | `/admin` | ✅ 保持不变 |
| 管理端文章 | AdminPostController | `/admin/post` | ✅ 保持不变 |
| 管理端配置 | AdminSiteConfigController | `/admin/site-config` | ✅ 保持不变 |
| 管理端统计 | AdminStatsController | `/admin/stats` | ✅ 保持不变 |
| 管理端媒体 | AdminMediaController | `/admin/media` | ✅ 保持不变 |
| 管理端分类 | AdminTaxonomyController | `/admin` | ✅ 保持不变 |
| 管理端日志 | AdminLogsController | `/admin/logs` | ✅ 保持不变 |
| 管理端动态 | AdminMomentsController | `/admin/moments` | ✅ 保持不变 |
| 管理端友链 | AdminLinksController | `/admin/links` | ✅ 保持不变 |
| 管理端AI | AdminAssistantController | `/admin/assistant` | ✅ 保持不变 |

### 前端 API 调用（需要统一）

当前前端混合使用两种格式：
- 老格式：`/api/xxx`
- 新格式：`/xxx`

**统一方案**：全部使用 `/api/xxx` 格式，符合 RESTful API 规范

## 统一规范

### 1. 后端保持不变

后端所有控制器路径保持不变（无前缀），例如：
- `/site/config`
- `/post/list`
- `/admin/post/page`

### 2. 前端统一使用 `/api` 前缀

所有前端 API 调用统一加上 `/api` 前缀：
- `/api/site/config`
- `/api/post/list`
- `/api/admin/post/page`

### 3. Nginx 配置统一转发

```nginx
# 前台接口统一前缀 /api
location /api/ {
    proxy_pass http://localhost:8080/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# 管理端接口统一前缀 /api/admin
# （已包含在上面的 /api/ 中，无需额外配置）

# 特殊处理：AI助手流式接口
location = /api/assistant/chat/stream {
    proxy_pass http://localhost:8080/assistant/chat/stream;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
    gzip off;
}
```

## 实施步骤

### 步骤 1：修改前端 API 调用（统一加 /api 前缀）

修改文件：`GodLeiBlog-Frontend/src/api/index.js`

所有接口调用改为带 `/api` 前缀：
- `/logs` → `/api/logs`
- `/post/list` → `/api/post/list`
- `/site/config` → `/api/site/config`
- 等等...

### 步骤 2：检查管理端 API 调用

检查 `GodLeiBlog-AdminPanel/src/api/` 下的文件，确保管理端接口也统一使用 `/api` 前缀。

### 步骤 3：更新 Nginx 配置

简化 nginx 配置，只保留 `/api/` 通用 location。

### 步骤 4：重新打包前端

- 打包前台前端
- 打包管理端

### 步骤 5：部署到服务器

- 上传前端文件
- 上传 nginx 配置
- 重载 nginx

### 步骤 6：验证测试

测试所有接口是否正常访问。

## 预期结果

- 所有接口统一使用 `/api/xxx` 格式
- 后端代码无需修改
- Nginx 配置简化
- 前后端接口规范统一
