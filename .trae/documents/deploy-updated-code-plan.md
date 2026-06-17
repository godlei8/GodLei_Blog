# 重新部署更新代码到服务器计划

## 概述
用户更新了多处代码，需要重新部署一版到服务器（8.137.187.70）。

## 主要变更点

### 1. Twikoo 服务配置更新
- **文件**: `twikoo-server.js`
- **变更**: 使用环境变量优先模式，支持 `LIMIT_PER_MINUTE` 和 `LIMIT_PER_MINUTE_ALL` 限流配置
- **部署操作**: 更新 `/opt/twikoo/server.js` 并重启服务

### 2. 前端环境变量变更
- **文件**: `GodLeiBlog-Frontend/.env`
- **变更**: 
  - `VITE_TWIKOO_URL=/twikoo-proxy/` (使用同域代理)
  - `VITE_TWIKOO_PROXY_TARGET=http://localhost:3000`
- **部署操作**: 构建前端并部署到 `/opt/godleiblog/frontend/`

### 3. 管理端环境变量变更
- **文件**: `GodLeiBlog-AdminPanel/.env`
- **变更**: 与前端保持一致，使用 `/twikoo-proxy/` 路径
- **部署操作**: 构建管理端并部署到 `/opt/godleiblog/frontend/admin/`

### 4. Nginx 配置更新
- **文件**: `nginx.conf`
- **变更**: 新增 `/twikoo-proxy/` 路径代理到 `localhost:3000`
- **部署操作**: 更新 `/etc/nginx/conf.d/godleiblog.conf` 并重载

### 5. Systemd 服务配置更新
- **文件**: `deploy/aliyun-ecs/systemd/twikoo.service`
- **变更**: 添加环境变量 `LIMIT_PER_MINUTE=0` 和 `LIMIT_PER_MINUTE_ALL=0`
- **部署操作**: 更新 `/etc/systemd/system/twikoo.service` 并重启

## 部署步骤

### 阶段一：构建前端代码
1. 构建前台前端 `GodLeiBlog-Frontend`
2. 构建管理端 `GodLeiBlog-AdminPanel`
3. 构建后端 `GodLeiBlog-Backend`（如有更新）

### 阶段二：部署到服务器
1. 部署 Twikoo 服务代码到 `/opt/twikoo/server.js`
2. 部署前端到 `/opt/godleiblog/frontend/`
3. 部署管理端到 `/opt/godleiblog/frontend/admin/`
4. 部署后端 JAR 到 `/opt/godleiblog/backend/`（如有更新）

### 阶段三：更新服务配置
1. 更新 Nginx 配置 `/etc/nginx/conf.d/godleiblog.conf`
2. 测试 Nginx 配置并重载
3. 更新 Twikoo systemd 配置 `/etc/systemd/system/twikoo.service`
4. 重启 Twikoo 服务
5. 重启后端服务（如有更新）

### 阶段四：验证
1. 测试首页访问
2. 测试评论功能
3. 测试管理端访问
4. 检查所有服务状态

## 关键配置对照

| 组件 | 本地开发 | 服务器部署 |
|------|----------|------------|
| 前端 Twikoo URL | `/twikoo-proxy/` | `/twikoo-proxy/` |
| Nginx Twikoo 代理 | - | `/twikoo-proxy/` → `localhost:3000` |
| Twikoo 服务 | `localhost:3000` | `localhost:3000` |
| 限流配置 | `LIMIT_PER_MINUTE=0` | `LIMIT_PER_MINUTE=0` |

## 回滚方案
如部署失败，可快速回滚：
1. 恢复之前的 JAR 包（如有备份）
2. 恢复之前的 Nginx 配置
3. 重启相关服务

## 预计时间
- 构建阶段：3-5 分钟
- 部署阶段：2-3 分钟
- 验证阶段：1-2 分钟
- 总计：约 10 分钟
