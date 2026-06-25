# 架构决策记录（ADR）

> 提架构改动前先看这里，避免和既有决策冲突。

### ADR-001: 文章详情改用后端结构化字段（2026-06-17）
**Context**: 前台原本靠客户端解析正文 front-matter 拿标签/分类/描述，脆弱。
**Decision**: `/post/get` 返回 `PostDetailVO`（分类路径数组、标签数组、上一篇/下一篇）；前台直接用结构化字段，front-matter 仅作兜底。
**Consequences**: 三端字段对齐、更稳；后端 `get` 需补选 `category_id` 并 JOIN 标签、回溯分类层级、查相邻文章。

### ADR-002: 对象存储用腾讯云 COS（2026-06-17）
**Context**: 原 `aliyun.oss.*` 是死配置；图片只存服务器本地。
**Decision**: 抽象 `StorageProvider`（local / cos），按 `upload.provider` 切换；COS 用官方 `com.qcloud:cos_api`；对象键加 `prefix`（=blog）命名空间。配置前缀统一为 `upload.*`。
**Alternatives**: 阿里云 OSS（弃，用户选腾讯云）；S3 兼容（弃，不如官方直接）。
**Consequences**: 密钥走环境变量 / 本地 `application-local.yml`（gitignore），绝不入库。

### ADR-003: 生产用 systemd + jar + nginx（非 Docker 流水线）（2026-06-17）
**Context**: `.github/workflows` 是 docker 部署，但服务器实际是 systemd。
**Decision**: 生产沿用 `/opt/godleiblog` 下 systemd 服务 + nginx 静态文件 + `SPRING_PROFILES_ACTIVE=prod` + `application-prod.yml`（无明文，全走环境变量）。
**Consequences**: 部署 = 备份 → 替换 jar/静态 → 改 backend.env → `systemctl restart` → 验证；想走 CI 需把 docker run 补 COS/助手 env 并切 prod。

### ADR-004: 后台重设计 = 墨+金编辑台，保留 Element Plus 改皮肤（2026-06-17）
**Decision**: 冷纸面 + 强墨字 + 金色点缀 + 绯红主操作；等宽字体记数据；扁平发丝面板。IA 合并:设置 4 页→一个「站点设置」页签（SiteConfigPageShell embedded 模式），分类/标签并入「文章」页签，导航 11→6。逐页**按 mockup 重搭结构**（不是只换色）。
**Consequences**: 参照 `mockups/admin-redesign.html`；EP 主色用 `!important` 改绯红（见 bugs）。

### ADR-005: 上传严格校验（2026-06-17）
**Decision**: 仅 jpg/png/gif/webp，按文件头**魔数**验真，禁 SVG，单图 ≤5MB（可配 `UPLOAD_MAX_IMAGE_SIZE`）。
