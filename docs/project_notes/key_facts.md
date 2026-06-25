# 关键事实 / 配置

> 配置、端口、URL、凭据**位置**。⚠️ 不写明文密钥/密码（本文件入库）——只记"在哪取"。

## 生产服务器与部署
- **服务器**: `8.137.187.70`（阿里云 Linux 8）；域名 `godlei8.top`（HTTPS）。root 可登录。
- **部署根**: `/opt/godleiblog/` → `backend/`(app.jar + backend.env)、`frontend/`、`admin/`、`data/`。
- **本地图片实际目录**: `/opt/godleiblog/backend/uploads`（不是 data/uploads）。
- **后端**: systemd 服务 `godleiblog-backend`，`EnvironmentFile=/opt/godleiblog/backend/backend.env`，`SPRING_PROFILES_ACTIVE=prod`，端口 `8080`。
- **MySQL**: 本机 `127.0.0.1:3306`，库 `godleiblog`（原生 mysqld，非容器）。
- **nginx**: `/etc/nginx/conf.d/godleiblog.conf` → `/api/`→8080、`/uploads/`→8080、`/twikoo/` `/twikoo-proxy/`→3000、HTTPS godlei8.top。
- **部署/SSH 自动化**: 本机用 `py + paramiko`（PuTTY plink 也在）；备份在 `/opt/godleiblog/_backup_*`。

## 存储（COS）
- 腾讯云 COS：bucket `godlei-1311686380`、region `ap-chengdu`、公网域名 `http://cos.godlei8.top`、对象前缀 `blog`。
- 后端配置前缀 `upload.*`；`UPLOAD_PROVIDER=cos`、`UPLOAD_DIR=/opt/godleiblog/backend/uploads`。
- **密钥位置**: 环境变量 `COS_SECRET_ID` / `COS_SECRET_KEY`；本地开发在 `application-local.yml`（已 gitignore）。

## 前端构建约定
- 前端 axios `baseURL` 默认 `/api`（`utils/apiBase.js`），各 API 路径**不含** `/api`。
- 生产构建用 `GodLeiBlog-Frontend/.env.production`：`VITE_API_BASE_URL=/api`、`VITE_TWIKOO_URL=/twikoo-proxy/`。
- 管理端 base `/admin/`；二者经 nginx 同源 `/api` 反代到 8080（剥掉 /api）。

## 后台 / 鉴权 / 助手
- 后台登录默认 `admin / admin`（建议改）；`/admin/*` 接口靠 **`token` 请求头** 鉴权（TokenFilter）。
- 馨宝 AI 助手：默认 provider `deepseek`；API key 解析顺序 = 后台 DB（blog_site_setting）> 环境变量 `BLOG_ASSISTANT_API_KEY`。**线上目前未配 key，无法对话。**

## Git
- remote `godlei` = `github.com/godlei8/GodLei_Blog`；分支 `dev`（开发）、`master`（旧/CI docker 流水线）。

## ⚠️ 安全待办（凭据轮换）
- root 密码、DB 密码、COS 密钥曾在协作对话中出现 → **建议全部轮换**。
- 私钥 `server_key.pem` 已从跟踪移除，但仍在 git 历史 + xinbao 分支里 → 建议轮换该密钥（彻底清理需 filter-repo + 强推）。
