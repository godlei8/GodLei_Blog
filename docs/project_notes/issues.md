# 工作日志

> 已完成 / 进行中 / 待办。新会话开局看这里，知道前面干到哪、还剩什么。

## 已完成
### 2026-06-17 — 文章详情页三端重构
- **Status**: Completed
- 后端新增 `PostDetailVO`/`PostNeighbor`，`/post/get` 返回结构化分类/标签/相邻文章；前台 `PostDetail.vue` 全量重写（接主题色、阅读进度、字数/时长、相邻导航、目录 sticky 后改 fixed 常驻）；管理端编辑回显以结构化字段为准。
- 见 `docs/superpowers/specs/2026-06-17-post-detail-redesign-design.md`、`mockups/post-detail-redesign.html`。

### 2026-06-17 — 合并 codex/xinbao-ai-assistant 到 dev
- **Status**: Completed
- 带入：馨宝 AI 助手、朋友圈 Moments、管理端改版。冲突仅 router/PostDetail（保留重设计 + 嫁接 `setPageContext`）。
- 应用了 stash 里"适配多模型"优化（`OpenAiCompatibleChatClient` + 运行时/体验配置），并按端拆 4 个 commit。

### 2026-06-17 — 接入腾讯云 COS + 严格上传校验 + 历史图迁移
- **Status**: Completed（线上已迁移 4 张结构化引用图到 COS；本地 uploads 已取消跟踪，文件保留）。

### 2026-06-17 — 首次上线生产
- **Status**: Completed
- systemd + nginx 部署到 8.137.187.70；后端/首页/文章页/管理端/API 全 200；生产库图片迁移 COS；新增 `application-prod.yml`。

### 2026-06-17 — 登录页换背景图
- **Status**: Completed（`login-admin-bg-v4.jpg`，Unsplash 落霞天幕；登录布局保留）。

### 2026-06-22 — 后台重设计
- **Status**: Mostly done
- 阶段一(设计系统+外壳)、阶段二(逐页统一令牌)、阶段三(IA 合并:设置4→1、分类标签并入文章、导航→6)；控制台按 mockup 重搭；**修正 EP 主色为绯红**(见 bugs)。
- 用 `admin/admin` + Playwright 登录线上截图核对：控制台/文章/设置已对齐 `mockups/admin-redesign.html`。

## 待办 / Open
- **馨宝缺 `BLOG_ASSISTANT_API_KEY`** → 线上无法对话，等用户给 key（DeepSeek/MiniMax 等）后写入 backend.env 重启。
- 后台**其余页**（朋友圈/友链/日志/文章编辑器）还没逐张对照 mockup 重搭结构（目前继承了全局皮肤，但内部结构未逐一核对）。
- **安全轮换**：root / DB / COS 密钥（见 key_facts）。
- 可选：用 git filter-repo 把 `server_key.pem` 从历史彻底清除。
