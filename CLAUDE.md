# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

GodLeiBlog —— 前后端分离的个人博客。单一 git 仓库，包含三个可部署项目和一个本地评论服务：

- `GodLeiBlog-Frontend/` —— 前台站点（Vue 3 + Vite，纯手写 CSS，无 UI 框架）
- `GodLeiBlog-AdminPanel/` —— 后台管理（Vue 3 + Vite + Element Plus），部署在 `/admin/` 子路径下
- `GodLeiBlog-Backend/` —— REST API（Spring Boot 3.2，Java 17，多模块 Maven）
- `twikoo-local/server.js` —— 本地 Twikoo 评论服务（端口 3000），用于开发

本机主系统为 Windows，辅助脚本均为 PowerShell。后端 Maven 通过 `mvnw.cmd` 包装器运行。

## 常用命令

### 一键启动全部（本地开发推荐）
```powershell
scripts/start-local.ps1   # 确保 MySQL 库+表存在，构建并启动后端(8080)、前台(5173)、管理端(5174)
scripts/stop-local.ps1
```
`start-local.ps1` 会自动创建 `godleiblog` 库，若缺少 `blog_post` 表则导入 `GodLeiBlog建表.sql`。它需要 MySQL **已在运行**，并通过环境变量（`HM_MYSQL_EXE`、`HM_JAVA_EXE`、`JAVA_HOME`）或硬编码兜底路径（`D:\Kf\mysql\bin`、`D:\Kf\jdk21\bin`）定位 `mysql.exe`/`java.exe`。数据库凭据来自 `HM_LOCAL_DB_*` 环境变量（见 `scripts/import-local-env.ps1`）。日志输出到 `.run/`。

### 后端（Spring Boot 多模块）
```powershell
scripts/run-backend.ps1            # 以 profile=local 打包并运行 blog-server
scripts/run-backend-debug.ps1
```
手动执行 Maven（在 `GodLeiBlog-Backend/` 目录下，务必构建整个 reactor 以解析兄弟模块）：
```powershell
.\mvnw.cmd -pl blog-server -am package -DskipTests    # 构建可运行 jar
.\mvnw.cmd test                                       # 运行测试（仅 blog-server 有测试类）
.\mvnw.cmd -pl blog-server test -Dtest=BlogServerApplicationTests   # 运行单个测试
```
可运行产物为 `blog-server/target/blog-server-0.0.1-SNAPSHOT.jar`。**`blog-server` 是唯一的应用模块** —— `blog-common`（过滤器/JWT/Result/CORS）和 `blog-pojo`（实体/DTO）是库模块。忽略 `GodLeiBlog-Backend/src/...` 下的 `GodLeiBlogBackendApplication`：父工程是 `packaging: pom`，那段源码是遗留物，并非真正的入口。

### 前台 / 管理端（各自独立的 npm 项目）
```powershell
npm install
npm run dev      # 前台: 5173 ; 管理端: 5173（与前台同时运行时用 --port 5174）
npm run build
```
两个 Vue 项目均未配置 lint/test —— 只有 `dev`、`build`、`preview` 三个脚本。

## 架构

### 请求路径与 API 前缀约定
两个 Vue 应用都通过一个**服务端并不存在的 `/api` 前缀**调用后端 —— Vite 代理（开发）和 Nginx（生产）会在转发到 `:8080` 前去掉 `/api`。因此 [src/api/index.js](GodLeiBlog-Frontend/src/api/index.js) 中的 `request('GET', '/api/post/list')` 实际命中后端的 `/post/list`。新增接口时，前端路径 = `/api` + 后端的 `@RequestMapping`。axios 的 `baseURL` 是 `/`（同源），除非 `VITE_API_BASE_URL` 覆盖 —— 切勿硬编码 `localhost`，那会破坏移动端/生产环境。Vite 还代理了 `/uploads`、`/post`、`/logs`、`/links/list` 和 `/twikoo-proxy`。

### 后端分层（`cn.godlei.blogserver`）
`controller → service（接口 + Impl）→ mapper（MyBatis 接口 + XML）`。Controller 按受众拆分：
- **`controller/user/`** —— 公开接口，映射在 `/post`、`/logs`、`/links`、`/site`、`/post/stats`。
- **`controller/admin/`** —— 映射在 **`/admin/*`** 下，受鉴权保护（见下文）。

MyBatis mapper XML 位于 `blog-server/src/main/resources/cn/godlei/blogserver/mapper/*.xml`（与接口包结构镜像）。已开启 `map-underscore-to-camel-case`；分页由 PageHelper 提供（`pageNum`/`pageSize`）。所有响应统一用 `Result` 包装，分页用 `PageResult`（位于 `blog-pojo`/`blog-common`）。

### 鉴权模型
`TokenFilter`（位于 `blog-common`，是一个作用于 `urlPatterns = "/admin/*"` 的 `@WebFilter`，通过 `BlogServerApplication` 上的 `@ServletComponentScan` 启用）保护所有 admin 路由。它读取 **`token` 请求头**（不是 `Authorization`/Bearer），用 `JwtUtil` 校验，放行含 `login` 的路径，其余返回 401。在 `/admin/...` 下新增功能会自动受保护；管理端必须在 `token` 请求头中携带 JWT。

### 媒体 / 存储
上传通过 `blog.storage.mode` 抽象（默认 `local`，或阿里云 OSS）。本地模式写入 `${user.dir}/uploads`，并通过 `/uploads` 提供访问（`UploadStaticResourceConfig` + Vite/Nginx 代理）。OSS 配置位于 `aliyun.oss.*`。

### 评论（Twikoo）
评论由外部 Twikoo 处理，不存储在本后端。开发环境通过 `.env`（`VITE_TWIKOO_URL` / `VITE_TWIKOO_PROXY_TARGET`）指向 `http://localhost:3000`；`/twikoo-proxy` 这个 Vite 路由用于规避对远程 Twikoo 实例的 CORS。前台会聚合文章评论、留言板评论和友链评论。

## 配置

后端数据库 URL 由 `hm.db.host` + `hm.db.password` 拼装（见 `application.yml` → `jdbc:mysql://${hm.db.host}:3306/godleiblog`）。Profile：
- **`local`** —— 所有运行脚本使用，读取 `HM_LOCAL_*` 环境变量（数据源 URL、凭据、上传目录）。它依赖的 `application-local.yml` 已被 git 忽略 —— 请通过环境变量提供，而非提交文件。
- **`dev`** —— `host: mysql`，`password: ${HM_DB_PASSWORD}`（容器/compose）。

切勿提交数据库密码或 JWT 密钥 —— 它们均通过环境变量注入。

## CI/CD 与部署

`.github/workflows/` 有三条独立流水线：`deploy-backend.yml`、`deploy-frontend.yml`、`deploy-adminpanel.yml`（构建 + SSH 部署）。生产参考配置（Nginx vhost、systemd unit、安装/启动脚本）位于 `deploy/aliyun-ecs/`。数据库设计文档见 `docs/DATABASE_DESIGN.md`（及 `_EN` 英文版）。
