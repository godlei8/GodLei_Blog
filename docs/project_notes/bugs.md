# Bug 日志（含解决方案）

> 记录踩过的坑、根因与修法，避免重复。新会话遇到类似报错先来这里搜。

### 2026-06-17 — 后台整体仍像默认 Element Plus（主色是蓝的）
- **Issue**: 后台重设计后，主按钮/激活态/分页仍是 EP 默认蓝，整体不像 mockup。
- **Root Cause**: Element Plus 运行时注入的 `:root{--el-color-primary:#409eff}` 在加载顺序上压过了主题里设的绯红。
- **Solution**: `GodLeiBlog-AdminPanel/src/assets/main.css` 用 `:root, html { --el-color-primary: #7a1d2d !important; ... }`（success 并入金色 `#a9772b`）。
- **Prevention**: 覆盖 EP 的 CSS 变量时用 `!important`，不要只靠源序。

### 2026-06-17 — 部署后本地图片 404（封面/头像/正文图）
- **Issue**: 新后端部署后 `/uploads/...` 图片打不开。
- **Root Cause**: 生产实际图片在 `/opt/godleiblog/backend/uploads`，但我把 `UPLOAD_DIR` 设成了空目录 `/opt/godleiblog/data/uploads`。
- **Solution**: `backend.env` 改 `UPLOAD_DIR=/opt/godleiblog/backend/uploads`，重启 `godleiblog-backend`。
- **Prevention**: 部署前确认旧 `HM_LOCAL_UPLOAD_DIR` 实际指向哪、文件在哪。

### 2026-06-17 — 私钥 server_key.pem 被纳入 git
- **Issue**: 私钥进了版本库并推到远端。
- **Root Cause**: `.gitignore` 写的是 `server-key.pem`（连字符），真实文件名是 `server_key.pem`（下划线），规则没匹配上。
- **Solution**: 修正 `.gitignore` 加 `server_key.pem`、`*.pem`、`out/`、`*.iml`；`git rm --cached server_key.pem`。**密钥已暴露在历史里，需轮换。**
- **Prevention**: gitignore 凭据后用 `git check-ignore <file>` 验证真生效。

### 2026-06-17 — 前台生产构建失败（FriendsCircle.vue 缺失）
- **Issue**: `npm run build` 报 `Could not load .../FriendsCircle.vue`。
- **Root Cause**: `router/index.js` 引用了从未存在的 `FriendsCircle.vue`（`/fc` 预留路由），且无任何导航指向它。
- **Solution**: 合并 xinbao 后用真正的 `Moments.vue`/`/moments` 取代；移除悬空的 `/fc` import 与路由。
- **Prevention**: 路由引用的组件必须存在；删功能时连 import + 路由一起清。

### 2026-06-17 — 部署打包误传旧包（tar 路径错）
- **Issue**: 改了代码却部署上去的是旧版本。
- **Root Cause**: `cd` 进子目录后用仓库根相对路径 `tar`，失败但 sftp 又传了上一次残留的 tar。
- **Solution**: 始终**从仓库根**打包：`tar -czf .run/deploy/x.tar.gz -C <dist> .`，并核对部署后 asset 哈希与本地构建一致。
- **Prevention**: 部署脚本统一在仓库根执行；部署后 `grep index.<hash>.js` 比对。
