# API 兼容性方案 - 新老接口同时支持

## 问题背景

当前项目存在接口路径不一致的问题：
- 老接口：前端调用 `/api/xxx`，后端控制器路径 `/xxx`
- 新接口：前端调用 `/xxx`，后端控制器路径 `/xxx`

需要一套方案让新老接口都能正常工作。

## 现状分析

### 后端控制器路径（统一无前缀）
| 控制器 | 路径前缀 | 接口示例 |
|--------|----------|----------|
| SiteConfigController | `/site` | `/site/config` |
| PostController | `/post` | `/post/list`, `/post/get` |
| SiteStatsController | `/post/stats` | `/post/stats`, `/post/stats/track` |
| LogsController | `/logs` | `/logs` |
| LinksController | `/links` | `/links/list` |
| MomentsController | `/moments` | `/moments/list`, `/moments/get` |
| AssistantController | `/assistant` | `/assistant/chat/stream` |
| AdminAuthController | `/admin` | `/admin/login` |
| AdminPostController | `/admin/post` | `/admin/post/page` |
| AdminSiteConfigController | `/admin/site-config` | `/admin/site-config/get` |
| AdminStatsController | `/admin/stats` | `/admin/stats/dashboard` |
| AdminMediaController | `/admin/media` | `/admin/media/upload` |
| AdminTaxonomyController | `/admin` | `/admin/taxonomy/tree` |
| AdminLogsController | `/admin/logs` | `/admin/logs/page` |
| AdminMomentsController | `/admin/moments` | `/admin/moments/page` |
| AdminLinksController | `/admin/links` | `/admin/links/page` |
| AdminAssistantController | `/admin/assistant` | `/admin/assistant/models` |

### 前端调用路径（需要兼容两种）
- 老版本：所有接口带 `/api` 前缀，如 `/api/site/config`
- 新版本：所有接口不带 `/api` 前缀，如 `/site/config`

## 推荐方案：Nginx 双路径支持

通过 Nginx 配置同时支持两种路径格式，无需修改后端代码。

### Nginx 配置策略

```nginx
# 方案：保留 /api/ 前缀的 location，同时支持不带 /api/ 的访问

# 1. 老接口兼容：/api/site/config → 转发到后端 /site/config
location /api/ {
    proxy_pass http://localhost:8080/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# 2. 新接口支持：/site/config → 转发到后端 /site/config
location /site/ {
    proxy_pass http://localhost:8080/site/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /post/ {
    proxy_pass http://localhost:8080/post/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /logs/ {
    proxy_pass http://localhost:8080/logs/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /links/ {
    proxy_pass http://localhost:8080/links/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /moments/ {
    proxy_pass http://localhost:8080/moments/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /assistant/ {
    proxy_pass http://localhost:8080/assistant/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /uploads/ {
    proxy_pass http://localhost:8080/uploads/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /admin/ {
    proxy_pass http://localhost:8080/admin/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

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

location = /assistant/chat/stream {
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

### 步骤 1：更新 Nginx 配置
1. 修改 nginx.conf，添加 `/api/` 通用 location 和各独立 location
2. 上传到服务器
3. 测试并重载 nginx

### 步骤 2：前端代码保持现状
- 前台前端：使用新路径（不带 `/api` 前缀）
- 管理端：检查是否需要调整

### 步骤 3：验证测试
测试以下接口都能正常访问：
- 老接口：`/api/site/config`, `/api/post/list`, `/api/post/stats`
- 新接口：`/site/config`, `/post/list`, `/post/stats`

## 优势

1. **无需修改后端代码** - 保持后端控制器路径不变
2. **兼容新老接口** - 同时支持两种调用方式
3. **灵活过渡** - 可以逐步迁移前端代码，无需一次性全改
4. **管理端不受影响** - `/admin/*` 路径保持原样

## 注意事项

1. `/api/` location 必须放在其他具体 location 之前或之后，注意优先级
2. 确保 `/api/assistant/chat/stream` 的特殊配置不被通用 `/api/` 覆盖
