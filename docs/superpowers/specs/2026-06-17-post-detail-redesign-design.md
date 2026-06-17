# 文章详情页重构设计

- 日期：2026-06-17
- 状态：待评审
- 视觉稿：[mockups/post-detail-redesign.html](../../../mockups/post-detail-redesign.html)
- 涉及端：前台（GodLeiBlog-Frontend）、后端（GodLeiBlog-Backend）、管理端（GodLeiBlog-AdminPanel）

## 1. 背景与目标

当前文章详情页 [PostDetail.vue](../../../GodLeiBlog-Frontend/src/views/PostDetail.vue) 存在两类问题：

1. **视觉与全站割裂**：页面用写死的灰色（`#2d2d2d`、`#1e1e1e`）和一道白边框，完全没有接入全站主题变量（绯红 `#4a121d` + 金 `#d6ad5c`，见 [main.css](../../../GodLeiBlog-Frontend/src/assets/main.css)），观感比首页/页脚明显粗糙。
2. **数据来源脆弱**：标签、分类、描述靠前端 `front-matter` 客户端解析（`fm(this.post.content)`），而后端其实已经把这些结构化存储在 `blog_category`（层级）+ `blog_post_tag` 关联表里，只是 `/post/get` 没返回。

**目标**：按已确认的视觉稿重构文章页，使其与全站统一；同时把元信息数据来源从"前端解析 front-matter"切换为"后端结构化字段"，并补齐相邻文章导航。三端字段对齐。

## 2. 设计方向（已确认）

视觉稿已与用户迭代确认，最终方向：

- 顶部**阅读进度条**；沉浸式标题区（金色渐变标题，已调小到 `clamp(23px, 3vw, 32px)`）+ kicker 标签。
- **元信息条**：分类/标签胶囊 chip、更新时间、字数、预计阅读时长（带图标）。
- **正文排版重做**：行高 1.85，h2 金色侧标，引用块绯红渐变，行内代码/代码块/表格/图片全部接入主题色。
- **右侧目录**：`position: sticky` 毛玻璃面板，滚动高亮当前章节，一级（h2）方块标记、二级（h3）树状连接线区分层级（字号已缩小）。
- **悬浮操作球**：圆形金绯渐变 + 图标（回顶 / 评论；移动端额外目录）。
- 文末：标签、**版权声明**、**上一篇/下一篇**。

## 3. 范围（三端联动）

| 端 | 改动量 | 核心内容 |
|---|---|---|
| 后端 | 中 | `/post/get` 返回结构化分类路径 + 标签数组 + 相邻文章；新增 response VO 与 mapper 查询 |
| 前台 | 大 | 重写 `PostDetail.vue`（视觉 + 改用结构化字段 + 阅读元信息派生 + 相邻导航 + 版权） |
| 管理端 | 小 | 编辑回显改为以后端结构化 `categories`/`tags` 为准（与展示端口径一致），可选对齐预览样式 |

## 4. 数据契约变更（关键）

新增文章详情响应对象（**不改 `Post` 实体**，新增 response DTO，避免污染写入模型）：

```
PostDetailVO {
  // 现有 Post 字段
  String id;
  String title;
  String content;
  String cover;
  String description;
  LocalDateTime createTime;
  LocalDateTime updateTime;
  // 新增结构化字段
  List<String> categories;   // 分类路径，从父到子，如 ["DevOps"]；无则空数组
  List<String> tags;         // 标签名数组；无则空数组
  PostNeighbor prev;         // 上一篇（更早发布），无则 null
  PostNeighbor next;         // 下一篇（更晚发布），无则 null
}

PostNeighbor { String id; String title; }
```

相邻文章语义：以 `create_time` 排序，**上一篇 = 比当前更早发布的最近一篇**，**下一篇 = 比当前更晚发布的最近一篇**；不存在则为 `null`，前端隐藏对应卡片。

## 5. 后端改动（blog-pojo + blog-server）

1. **新增 DTO**：`blog-pojo` 下 `dto/response/PostDetailVO.java`、`PostNeighbor`（或内嵌静态类）。
2. **PostMapper.xml 新增查询**：
   - `listTagNamesByPostId(postId)`：`JOIN blog_post_tag + blog_tag` 取标签名。
   - 分类路径：用 `category_id` 向上回溯 `blog_category.parent_id`（复用/对齐 `resolveCategoryId` 的逆过程）。优先在 service 用现有/新增 `findById` 循环拼路径；实现时确认 `BlogCategoryMapper` 是否已有按 id 查询，没有则补。
   - `findPrevByCreateTime(createTime)`：`WHERE create_time < #{createTime} ORDER BY create_time DESC LIMIT 1`。
   - `findNextByCreateTime(createTime)`：`WHERE create_time > #{createTime} ORDER BY create_time ASC LIMIT 1`。
   - 注意：`get` 查询需补选 `category_id`（当前未选），以便回溯分类路径。
3. **PostService / PostServerImpl**：新增 `getDetail(id)`，在现有 `get` 基础上组装 categories/tags/prev/next，返回 `PostDetailVO`。
4. **PostController（user）`/post/get`** 改为返回 `PostDetailVO`。
5. **AdminPostController `/post/get`**：保持返回 `Post` 即可（编辑回显不需要 prev/next）；若希望与前台口径完全一致，可同样返回带 categories/tags 的 VO（向后兼容，多余字段前端忽略）。

## 6. 前台改动（GodLeiBlog-Frontend）

1. **重写 [PostDetail.vue](../../../GodLeiBlog-Frontend/src/views/PostDetail.vue)** 模板 + 样式，落地视觉稿（全量使用 `--theme-accent-*` 变量）。
2. **数据来源切换**：标题/描述/分类/标签直接读 `post.categories` / `post.tags` / `post.description`，**不再** `fm()` 解析这些字段。正文仍需从 content 中剥离 front-matter（保留 `fm()` 仅用于分离 body 与渲染），TOC 提取、代码块增强、图片兜底逻辑沿用现有实现。
3. **阅读元信息派生**：前端从正文计算字数（中文按字符、英文按词）与预计阅读时长（约 300 字/分钟），不落库。
4. **相邻导航**：渲染 `post.prev` / `post.next`，点击路由到对应文章；为空则隐藏。
5. **版权声明**：通过现有 `loadSiteConfig()` 取 `basic.siteName` 作为署名（**SiteConfig 暂无 author 字段**）。文案默认："本文由 {siteName} 原创，转载请注明出处。"
6. **目录定位**：由现有脆弱的 JS 计算 `left` 改为 CSS `position: sticky`，移除 `updateTocPosition` / resize 监听相关逻辑；移动端抽屉行为保留。

## 7. 管理端改动（GodLeiBlog-AdminPanel）

1. **编辑回显口径对齐**：[AddArticle.vue](../../../GodLeiBlog-AdminPanel/src/views/AddArticle.vue) `loadArticle()` 在拿到详情后，优先用后端返回的结构化 `tags`/`categories` 回填表单（当前仅靠 `parseFrontMatter` 从正文解析），保证"编辑看到的 = 实际存储的"。front-matter 解析作为兜底保留。
2. **（可选）预览样式对齐**：右侧预览暂用裸 `markdown-it` 渲染，不含代码块头部/复制按钮。可后续将前台的代码块/表格渲染规则抽成共享逻辑，本次不强制。

## 8. 非目标（YAGNI）

- 不引入新的写作字段（阅读时长/字数均前端派生，不落库）。
- 不新增 `basic.author` 字段（除非评审时另行确认；本次用 `siteName` 署名）。
- 不做点赞/阅读量展示（数据层无对应字段）。
- 不重构首页列表卡片、归档页等其他页面。
- 管理端预览样式对齐为可选项，不阻塞本次。

## 9. 风险与回滚

- **风险**：`/post/get` 响应形状变化可能影响其它消费方。已核查仅前台文章页与管理端编辑回显消费该接口；新增字段向后兼容，旧字段不变，风险低。
- **回滚**：前端为单文件重写，保留原 `PostDetail.vue` 于 git 历史；后端 VO 为新增类，不删改写入路径，可独立回退。

## 10. 验收标准

1. 文章页视觉与稿一致，全程使用主题变量，移动端正常。
2. 分类/标签/描述来自后端结构化字段（断网 front-matter 也能正确显示）。
3. 字数与预计阅读时长显示合理。
4. 有相邻文章时上一篇/下一篇可见且跳转正确；边界（首篇/末篇）正确隐藏。
5. 版权区显示站点名。
6. 代码复制、TOC 高亮/折叠、悬浮球、阅读进度条均可用。
7. 管理端编辑文章时，标签/分类回显为实际存储值。
8. 后端 `mvnw -pl blog-server -am package` 通过；三端本地联调（`scripts/start-local.ps1`）可正常打开文章页。
