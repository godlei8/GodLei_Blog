<template>
  <div class="post-page">
    <!-- 顶部阅读进度条 -->
    <div class="read-progress" :style="{ width: readProgress + '%' }"></div>

    <div class="post-container">
      <!-- 文章主体（左侧） -->
      <main class="post-main">
        <article class="post-card">
          <!-- Hero 标题区 -->
          <header class="post-hero">
            <span class="post-kicker">ARTICLE</span>
            <h1 class="post-title">{{ displayTitle }}</h1>

            <div class="post-meta">
              <span class="meta-item" title="更新时间">
                <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M3 9h18M8 2v4M16 2v4"/></svg>
                更新于 {{ formatDate(displayUpdated) }}
              </span>
              <span class="meta-divider"></span>
              <span class="meta-item" title="预计阅读时长">
                <svg viewBox="0 0 24 24"><path d="M12 8v5l3 2"/><circle cx="12" cy="12" r="9"/></svg>
                约 {{ readingStats.minutes }} 分钟阅读
              </span>
              <span class="meta-divider"></span>
              <span class="meta-item" title="字数">
                <svg viewBox="0 0 24 24"><path d="M4 5h16M4 12h16M4 19h10"/></svg>
                {{ readingStats.words.toLocaleString() }} 字
              </span>
            </div>

            <div class="post-taxonomy" v-if="displayCategories.length || displayTags.length">
              <span
                v-for="cat in displayCategories"
                :key="'cat-' + cat"
                class="chip chip--cat"
              >{{ cat }}</span>
              <span
                v-for="tag in displayTags"
                :key="'tag-' + tag"
                class="chip chip--tag"
              >{{ tag }}</span>
            </div>

            <div v-if="displayDescription" class="post-description">
              {{ displayDescription }}
            </div>
          </header>

          <!-- 正文 -->
          <div class="post-content" v-html="renderedMarkdown"></div>

          <!-- 文末：版权声明 -->
          <footer class="post-footer">
            <div class="post-copyright">
              <b>版权声明：</b>本文由 {{ authorName }} 原创，转载请注明出处。
            </div>
          </footer>

          <!-- 上一篇 / 下一篇 -->
          <nav class="post-nav" v-if="post.prev || post.next">
            <a
              v-if="post.prev"
              class="post-nav__item prev"
              @click="goToPost(post.prev.id)"
            >
              <div class="nav-dir">← 上一篇</div>
              <div class="nav-title">{{ post.prev.title }}</div>
            </a>
            <span v-else class="post-nav__placeholder"></span>

            <a
              v-if="post.next"
              class="post-nav__item next"
              @click="goToPost(post.next.id)"
            >
              <div class="nav-dir">下一篇 →</div>
              <div class="nav-title">{{ post.next.title }}</div>
            </a>
            <span v-else class="post-nav__placeholder"></span>
          </nav>

          <!-- 评论区域（Twikoo） -->
          <section id="comments" class="post-comments">
            <h2 class="post-comments-title">评论</h2>
            <div class="post-comment-card">
              <div id="tcomment-post" ref="twikooPost"></div>
            </div>
          </section>
        </article>
      </main>

      <!-- 右侧目录（可折叠） -->
      <aside class="toc-sidebar-container" v-if="headings.length">
        <div
          class="toc-sidebar"
          :class="{ 'toc-sidebar--mobile-open': isMobileTocOpen }"
        >
          <div class="toc-title">目录</div>
          <ul class="toc-list">
            <li
              v-for="h in headings"
              :key="h.anchor"
              class="toc-item"
              :class="[
                'toc-item--lvl' + Math.min(h.level, 6),
                { 'toc-item--active': h.anchor === activeAnchor }
              ]"
              v-show="!isHiddenByParent(h)"
              @click="scrollToAnchor(h.anchor)"
            >
              <span
                v-if="hasChildren(h)"
                class="toc-toggle"
                @click.stop="toggleCollapse(h.anchor)"
              >{{ isCollapsed(h.anchor) ? '▶' : '▼' }}</span>
              <span v-else class="toc-toggle toc-toggle-placeholder"></span>
              <span class="toc-link">{{ h.title }}</span>
            </li>
          </ul>
        </div>
      </aside>
    </div>

    <!-- 悬浮操作球 -->
    <div class="fabs">
      <button class="fab" title="评论" @click="scrollToComments">
        <svg viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      </button>
      <button class="fab" title="回到顶部" @click="scrollToTop">
        <svg viewBox="0 0 24 24"><path d="M12 19V5M5 12l7-7 7 7"/></svg>
      </button>
      <button class="fab fab--toc" title="目录" @click="toggleMobileToc">
        <svg viewBox="0 0 24 24"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/></svg>
      </button>
    </div>
  </div>
</template>

<script>
import { fetchPostById } from '@/api';
import { loadTwikoo, getTwikooEnvId } from '@/utils/twikoo';
import { loadSiteConfig } from '@/utils/siteConfig';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import c from 'highlight.js/lib/languages/c';
import cpp from 'highlight.js/lib/languages/cpp';
import css from 'highlight.js/lib/languages/css';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import markdown from 'highlight.js/lib/languages/markdown';
import plaintext from 'highlight.js/lib/languages/plaintext';
import python from 'highlight.js/lib/languages/python';
import shell from 'highlight.js/lib/languages/shell';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';
import fm from 'front-matter';
import 'highlight.js/styles/github-dark.css';
import { bindImageFallback, coverFallbackUrl, resolveImageUrl } from '@/utils/image';

[
  ['bash', bash],
  ['c', c],
  ['cpp', cpp],
  ['c++', cpp],
  ['css', css],
  ['html', xml],
  ['java', java],
  ['javascript', javascript],
  ['js', javascript],
  ['json', json],
  ['markdown', markdown],
  ['md', markdown],
  ['plaintext', plaintext],
  ['python', python],
  ['py', python],
  ['shell', shell],
  ['sh', shell],
  ['sql', sql],
  ['typescript', typescript],
  ['ts', typescript],
  ['vue', xml],
  ['xml', xml],
  ['yaml', yaml],
  ['yml', yaml]
].forEach(([name, language]) => {
  hljs.registerLanguage(name, language);
});

export default {
  name: 'PostDetail',
  props: ['id'],
  data() {
    return {
      post: {},
      frontMatter: {},
      headings: [],     // { level, title, anchor }
      collapsedMap: {}, // { [anchor]: boolean }
      isMobileTocOpen: false,
      activeAnchor: '',
      readProgress: 0,
      authorName: 'GodLei Blog',
      _onScrollBound: null
    };
  },
  computed: {
    // 正文（剥离 front-matter 后的 Markdown body）
    body() {
      const { body, attributes } = fm(this.post.content || '');
      this.frontMatter = attributes || {};
      return body;
    },
    // 标题：优先后端结构化字段，回退 front-matter
    displayTitle() {
      return this.post.title || this.frontMatter.title || '';
    },
    // 描述：优先后端结构化字段，回退 front-matter
    displayDescription() {
      return this.post.description || this.frontMatter.description || '';
    },
    // 分类：优先后端结构化数组，回退 front-matter
    displayCategories() {
      return this.normalizeList(
        (this.post.categories && this.post.categories.length)
          ? this.post.categories
          : this.frontMatter.category
      );
    },
    // 标签：优先后端结构化数组，回退 front-matter
    displayTags() {
      return this.normalizeList(
        (this.post.tags && this.post.tags.length)
          ? this.post.tags
          : this.frontMatter.tags
      );
    },
    displayUpdated() {
      return this.post.updateTime || this.frontMatter.updated || this.post.createTime || '';
    },
    // 字数与预计阅读时长（前端从正文派生，不落库）
    readingStats() {
      const text = this.body
        .replace(/```[\s\S]*?```/g, ' ')      // 去掉代码块
        .replace(/`[^`]*`/g, ' ')              // 去掉行内代码
        .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ') // 去掉图片
        .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1') // 链接保留文字
        .replace(/[#>*_~\-]/g, ' ');           // 去掉常见 markdown 标记
      const cjk = (text.match(/[一-龥぀-ヿ]/g) || []).length;
      const latinWords = (text.match(/[A-Za-z0-9]+/g) || []).length;
      const words = cjk + latinWords;
      const minutes = Math.max(1, Math.round(words / 300));
      return { words, minutes };
    },
    renderedMarkdown() {
      const md = new MarkdownIt({
        highlight: function (str, lang) {
          if (lang && hljs.getLanguage(lang)) {
            try {
              return hljs.highlight(str, { language: lang }).value;
            } catch (_) {}
          }
          return ''; // 不高亮时走默认转义
        }
      });

      // 自定义代码块渲染：增加头部信息 & 复制按钮
      md.renderer.rules.fence = (tokens, idx, options) => {
        const token = tokens[idx];
        const info = token.info ? token.info.trim() : '';
        const lang = info.split(/\s+/g)[0] || '';
        const rawCode = token.content;

        let highlighted = '';
        if (options.highlight) {
          highlighted = options.highlight(rawCode, lang) || '';
        }

        const finalCode = highlighted || md.utils.escapeHtml(rawCode || '');
        const langLabel = lang || 'Text';

        return `
<div class="code-block">
  <div class="code-block__header">
    <span class="code-block__dots"><i></i><i></i><i></i></span>
    <span class="code-block__lang">${langLabel}</span>
    <button class="code-block__copy" type="button">复制</button>
  </div>
  <pre class="code-block__body"><code class="hljs language-${langLabel.toLowerCase()}">${finalCode}</code></pre>
</div>`;
      };

      // 自定义图片渲染规则
      md.renderer.rules.image = function (tokens, idx) {
        const token = tokens[idx];
        const src = resolveImageUrl(token.attrGet('src'), coverFallbackUrl);
        const alt = token.content;
        return `<img src="${src}" alt="${alt}" class="post-content-image" loading="lazy" />`;
      };

      // 自定义标题渲染规则（添加锚点）
      md.renderer.rules.heading_open = (tokens, idx) => {
        const token = tokens[idx];
        const level = token.tag.slice(1);
        const nextToken = tokens[idx + 1];
        const title = nextToken.content;
        const anchor = this.makeAnchor(title);
        return `<h${level} id="${anchor}">`;
      };

      // 提取标题列表
      this.extractHeadings(this.body);

      // 渲染正文
      return md.render(this.body);
    }
  },
  watch: {
    // 切换文章（上一篇/下一篇）时重新加载
    id() {
      this.reload();
    },
    renderedMarkdown() {
      this.$nextTick(() => {
        this.enhanceCodeBlocks();
        this.enhanceContentImages();
        this.updateActiveAnchor();
      });
    }
  },
  methods: {
    normalizeList(value) {
      if (value == null) return [];
      if (Array.isArray(value)) return value.map(v => String(v).trim()).filter(Boolean);
      return String(value).split(',').map(v => v.trim()).filter(Boolean);
    },

    makeAnchor(title) {
      return String(title)
        .toLowerCase()
        .replace(/\s+/g, '-')
        .replace(/[^\w\-一-龥]/g, '');
    },

    // 提取标题：按 # 的真实数量作为层级（H1~H6），排除代码块中的 #
    extractHeadings(markdown) {
      const headings = [];
      const lines = (markdown || '').split('\n');
      let inCodeBlock = false;

      lines.forEach(line => {
        const trimmed = line.trim();
        if (trimmed.startsWith('```')) {
          inCodeBlock = !inCodeBlock;
          return;
        }
        if (inCodeBlock) return;

        const m = trimmed.match(/^(#{1,6})\s+(.+)$/);
        if (!m) return;

        const level = m[1].length;
        const title = m[2].trim();
        headings.push({ level, title, anchor: this.makeAnchor(title) });
      });

      this.headings = headings;
    },

    // 平滑滚动到锚点
    scrollToAnchor(anchor) {
      const el = document.getElementById(anchor);
      if (!el) return;
      const offset = 90;
      const top = window.pageYOffset + el.getBoundingClientRect().top - offset;
      window.scrollTo({ top, behavior: 'smooth' });
      if (window.innerWidth <= 900) {
        this.isMobileTocOpen = false;
      }
    },

    toggleCollapse(anchor) {
      this.collapsedMap = { ...this.collapsedMap, [anchor]: !this.collapsedMap[anchor] };
    },
    isCollapsed(anchor) {
      return !!this.collapsedMap[anchor];
    },
    hasChildren(h) {
      const idx = this.headings.findIndex(x => x.anchor === h.anchor);
      if (idx === -1) return false;
      for (let i = idx + 1; i < this.headings.length; i++) {
        const lv = this.headings[i].level;
        if (lv <= h.level) return false;
        if (lv > h.level) return true;
      }
      return false;
    },
    isHiddenByParent(h) {
      const idx = this.headings.findIndex(x => x.anchor === h.anchor);
      if (idx <= 0) return false;
      let curLevel = h.level;
      for (let i = idx - 1; i >= 0; i--) {
        const prev = this.headings[i];
        if (prev.level < curLevel) {
          if (this.collapsedMap[prev.anchor]) return true;
          curLevel = prev.level;
        }
      }
      return false;
    },

    formatDate(dateString) {
      if (!dateString) return '未知时间';
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return String(dateString);
      return date.toLocaleDateString('zh-CN');
    },

    scrollToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    scrollToComments() {
      const el = document.getElementById('comments');
      if (!el) return;
      const top = window.pageYOffset + el.getBoundingClientRect().top - 90;
      window.scrollTo({ top, behavior: 'smooth' });
    },
    toggleMobileToc() {
      this.isMobileTocOpen = !this.isMobileTocOpen;
    },

    // 跳转到相邻文章
    goToPost(id) {
      if (!id || id === this.id) return;
      this.$router.push(`/posts/${id}`);
    },

    // 滚动时更新进度条与目录高亮
    onScroll() {
      const doc = document.documentElement;
      const scrollable = doc.scrollHeight - doc.clientHeight;
      this.readProgress = scrollable > 0
        ? Math.min(100, (doc.scrollTop / scrollable) * 100)
        : 0;
      this.updateActiveAnchor();
    },
    updateActiveAnchor() {
      let current = '';
      for (const h of this.headings) {
        const el = document.getElementById(h.anchor);
        if (el && el.getBoundingClientRect().top < 120) {
          current = h.anchor;
        }
      }
      this.activeAnchor = current;
    },

    enhanceContentImages() {
      const images = this.$el?.querySelectorAll('.post-content img') || [];
      images.forEach((img) => bindImageFallback(img, coverFallbackUrl));
    },

    // 为代码块绑定复制事件
    enhanceCodeBlocks() {
      const blocks = this.$el.querySelectorAll('.code-block');
      blocks.forEach(block => {
        const btn = block.querySelector('.code-block__copy');
        if (!btn || btn.dataset.bound === 'true') return;
        btn.dataset.bound = 'true';
        btn.addEventListener('click', () => {
          const codeEl = block.querySelector('pre code');
          if (!codeEl) return;
          const text = codeEl.innerText;
          const setCopied = () => {
            const oldText = btn.innerText;
            btn.innerText = '已复制';
            btn.classList.add('code-block__copy--ok');
            setTimeout(() => {
              btn.innerText = oldText;
              btn.classList.remove('code-block__copy--ok');
            }, 2000);
          };
          if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(setCopied).catch(() => {});
          } else {
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
            textarea.select();
            try {
              document.execCommand('copy');
              setCopied();
            } catch (e) {
              console.warn('复制失败', e);
            } finally {
              document.body.removeChild(textarea);
            }
          }
        });
      });
    },

    async loadPost() {
      try {
        const response = await fetchPostById(this.id);
        this.post = response.data || {};
      } catch (error) {
        console.error('加载文章失败:', error);
        this.post = {};
      }
    },

    async initTwikoo() {
      await this.$nextTick();
      const el = this.$refs.twikooPost;
      if (!el) return;
      try {
        const tw = await loadTwikoo();
        await Promise.resolve(
          tw.init({
            envId: getTwikooEnvId(),
            el,
            path: `/posts/${this.id}`
          })
        );
      } catch (e) {
        console.error('文章页 Twikoo 初始化失败', e);
      }
    },

    // 切换文章时整体重载
    async reload() {
      this.collapsedMap = {};
      this.activeAnchor = '';
      await this.loadPost();
      window.scrollTo({ top: 0 });
      this.initTwikoo();
    }
  },
  async created() {
    await this.loadPost();
    // 版权署名取自站点配置
    try {
      const config = await loadSiteConfig();
      if (config?.basic?.siteName) {
        this.authorName = config.basic.siteName;
      }
    } catch (e) {
      console.warn('加载站点配置失败，版权署名使用默认值', e);
    }
  },
  async mounted() {
    this.initTwikoo();
    this._onScrollBound = () => this.onScroll();
    window.addEventListener('scroll', this._onScrollBound, { passive: true });
    this.$nextTick(() => this.onScroll());
  },
  beforeUnmount() {
    if (this._onScrollBound) {
      window.removeEventListener('scroll', this._onScrollBound);
      this._onScrollBound = null;
    }
  }
};
</script>

<style scoped>
.post-page {
  position: relative;
}

/* 顶部阅读进度条 */
.read-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  z-index: 2000;
  background: linear-gradient(90deg, var(--theme-accent), var(--theme-accent-strong));
  box-shadow: 0 0 12px var(--theme-accent-glow);
  transition: width 0.1s linear;
}

/* 布局 */
.post-container {
  display: grid;
  grid-template-columns: minmax(0, 760px) 264px;
  gap: 36px;
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 24px 80px;
  justify-content: center;
  align-items: start;
}

.post-main {
  min-width: 0;
}

/* 文章卡片 */
.post-card {
  position: relative;
  background: var(--theme-accent-panel);
  border: 1px solid var(--theme-accent-border-soft);
  border-radius: 18px;
  box-shadow: 0 24px 60px var(--theme-accent-shadow-strong);
  overflow: hidden;
}
.post-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--theme-accent-line-horizontal);
}

/* Hero 标题区 */
.post-hero {
  position: relative;
  padding: 40px 44px 28px;
  border-bottom: 1px solid var(--theme-accent-border-soft);
  background:
    radial-gradient(120% 80% at 0% 0%, rgba(122, 29, 45, 0.30), transparent 60%),
    radial-gradient(90% 70% at 100% 0%, rgba(214, 173, 92, 0.12), transparent 55%);
}
.post-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--theme-accent-text);
  margin-bottom: 16px;
}
.post-kicker::before {
  content: "";
  width: 22px;
  height: 1px;
  background: var(--theme-accent-strong);
}
.post-title {
  margin: 0;
  font-size: clamp(23px, 3vw, 32px);
  line-height: 1.3;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: var(--theme-accent-title-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 2px 30px rgba(214, 173, 92, 0.12);
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-top: 22px;
  font-size: 13.5px;
  color: var(--theme-accent-text-soft);
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  opacity: 0.92;
}
.meta-item svg {
  width: 15px;
  height: 15px;
  stroke: var(--theme-accent-strong);
  fill: none;
  stroke-width: 1.7;
}
.meta-divider {
  width: 1px;
  height: 14px;
  background: var(--theme-accent-border);
}

.post-taxonomy {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 13px;
  border-radius: 999px;
  font-size: 12.5px;
  background: var(--theme-accent-surface);
  border: 1px solid var(--theme-accent-border-soft);
  color: var(--theme-accent-text-soft);
  transition: all 0.18s ease;
  cursor: default;
}
.chip:hover {
  background: var(--theme-accent-surface-strong);
  border-color: var(--theme-accent-border);
  transform: translateY(-1px);
  color: var(--theme-accent-text-strong);
}
.chip--cat {
  color: var(--theme-accent-text);
  font-weight: 600;
}
.chip--cat::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--theme-accent-strong);
}
.chip--tag::before {
  content: "#";
  color: var(--theme-accent-strong);
  font-weight: 700;
}

.post-description {
  margin-top: 24px;
  padding: 14px 18px;
  border-left: 3px solid var(--theme-accent-strong);
  background: var(--theme-accent-surface-soft);
  border-radius: 0 10px 10px 0;
  color: var(--theme-accent-text-soft);
  font-style: italic;
  font-size: 15px;
}

/* 正文 */
.post-content {
  padding: 36px 44px 8px;
  font-size: 16.5px;
  line-height: 1.85;
  color: #ececec;
}
:deep(.post-content p) {
  margin: 0 0 18px;
}
:deep(.post-content h1),
:deep(.post-content h2),
:deep(.post-content h3),
:deep(.post-content h4),
:deep(.post-content h5),
:deep(.post-content h6) {
  scroll-margin-top: 90px;
  color: var(--theme-accent-text-strong);
  font-weight: 700;
}
:deep(.post-content h2) {
  position: relative;
  font-size: 25px;
  line-height: 1.4;
  margin: 40px 0 18px;
  padding-left: 16px;
}
:deep(.post-content h2)::before {
  content: "";
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 5px;
  border-radius: 3px;
  background: var(--theme-accent-line-vertical);
  box-shadow: 0 0 10px var(--theme-accent-glow);
}
:deep(.post-content h3) {
  font-size: 20px;
  margin: 30px 0 14px;
  color: var(--theme-accent-text-soft);
}
:deep(.post-content a) {
  color: var(--theme-accent-text);
  text-decoration: none;
  border-bottom: 1px solid var(--theme-accent-border);
}
:deep(.post-content a:hover) {
  color: var(--theme-accent-text-strong);
  border-color: var(--theme-accent-strong);
}
:deep(.post-content ul),
:deep(.post-content ol) {
  margin: 0 0 18px;
  padding-left: 26px;
}
:deep(.post-content li) {
  margin: 7px 0;
}
:deep(.post-content li::marker) {
  color: var(--theme-accent-strong);
}
:deep(.post-content strong) {
  color: var(--theme-accent-text-soft);
}

/* 行内代码 */
:deep(.post-content :not(pre) > code) {
  background: var(--theme-accent-surface);
  border: 1px solid var(--theme-accent-border-soft);
  padding: 2px 7px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--theme-accent-text-soft);
  font-family: 'JetBrains Mono', 'Fira Code', Menlo, Consolas, 'Courier New', monospace;
}

/* 引用块 */
:deep(.post-content blockquote) {
  margin: 22px 0;
  padding: 14px 20px;
  border-left: 4px solid var(--theme-accent);
  background: linear-gradient(90deg, rgba(122, 29, 45, 0.18), transparent);
  border-radius: 0 12px 12px 0;
  color: #d8c7c9;
}
:deep(.post-content blockquote p) {
  margin: 0;
}

/* 代码块 */
:deep(.code-block) {
  position: relative;
  margin: 24px 0;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--theme-accent-border-soft);
  background: linear-gradient(160deg, #1c1014, #0c0608);
  box-shadow: 0 14px 34px var(--theme-accent-shadow);
}
:deep(.code-block__header) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 14px;
  font-size: 12px;
  background: linear-gradient(90deg, rgba(74, 18, 29, 0.6), rgba(12, 6, 8, 0.9));
  border-bottom: 1px solid var(--theme-accent-border-soft);
}
:deep(.code-block__dots) {
  display: flex;
  gap: 6px;
}
:deep(.code-block__dots i) {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  display: inline-block;
}
:deep(.code-block__dots i:nth-child(1)) { background: #e06c5a; }
:deep(.code-block__dots i:nth-child(2)) { background: var(--theme-accent-strong); }
:deep(.code-block__dots i:nth-child(3)) { background: #7faf6a; }
:deep(.code-block__lang) {
  color: var(--theme-accent-text);
  letter-spacing: 0.08em;
  font-weight: 600;
  margin-left: auto;
}
:deep(.code-block__copy) {
  border: 1px solid var(--theme-accent-border-soft);
  background: var(--theme-accent-surface);
  color: var(--theme-accent-text-soft);
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}
:deep(.code-block__copy:hover) {
  background: var(--theme-accent-surface-strong);
  border-color: var(--theme-accent-border);
  color: #fff;
}
:deep(.code-block__copy--ok) {
  background: rgba(120, 170, 90, 0.25);
  border-color: rgba(120, 170, 90, 0.5);
  color: #d6f0c0;
}
:deep(.code-block__body) {
  margin: 0;
  padding: 16px 18px;
  overflow-x: auto;
  background: transparent;
}
:deep(.code-block__body code) {
  background: transparent;
  font-family: 'JetBrains Mono', 'Fira Code', Menlo, Consolas, 'Courier New', monospace;
  font-size: 13.5px;
  line-height: 1.7;
}

/* 表格 */
:deep(.post-content table) {
  width: 100%;
  border-collapse: collapse;
  margin: 24px 0;
  font-size: 14.5px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--theme-accent-border-soft);
}
:deep(.post-content th),
:deep(.post-content td) {
  padding: 11px 14px;
  text-align: left;
  border-bottom: 1px solid var(--theme-accent-border-soft);
}
:deep(.post-content th) {
  background: rgba(74, 18, 29, 0.45);
  color: var(--theme-accent-text-soft);
  font-weight: 700;
}
:deep(.post-content tr:last-child td) {
  border-bottom: none;
}
:deep(.post-content tbody tr:hover) {
  background: var(--theme-accent-surface-soft);
}

/* 图片 */
:deep(.post-content img) {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 22px auto;
  border-radius: 12px;
  border: 1px solid var(--theme-accent-border-soft);
  box-shadow: 0 10px 28px var(--theme-accent-shadow);
}

/* 文末版权 */
.post-footer {
  margin: 16px 44px 0;
  padding-top: 22px;
  border-top: 1px solid var(--theme-accent-border-soft);
}
.post-copyright {
  padding: 16px 18px;
  border-radius: 12px;
  font-size: 13px;
  background: var(--theme-accent-surface-soft);
  border: 1px dashed var(--theme-accent-border-soft);
  color: #cdbfb0;
  line-height: 1.7;
}
.post-copyright b {
  color: var(--theme-accent-text-soft);
}

/* 上一篇 / 下一篇 */
.post-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin: 24px 44px 8px;
}
.post-nav__item {
  display: block;
  padding: 16px 18px;
  border-radius: 12px;
  text-decoration: none;
  cursor: pointer;
  background: var(--theme-accent-surface-soft);
  border: 1px solid var(--theme-accent-border-soft);
  transition: all 0.18s ease;
}
.post-nav__item:hover {
  background: var(--theme-accent-surface);
  border-color: var(--theme-accent-border);
  transform: translateY(-2px);
}
.post-nav__placeholder {
  display: block;
}
.post-nav .nav-dir {
  font-size: 12px;
  color: var(--theme-accent-text);
  letter-spacing: 0.1em;
}
.post-nav .nav-title {
  margin-top: 6px;
  color: var(--theme-accent-text-soft);
  font-weight: 600;
  font-size: 15px;
}
.post-nav .next {
  text-align: right;
}

/* 评论 */
.post-comments {
  margin: 30px 44px 40px;
}
.post-comments-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 21px;
  color: var(--theme-accent-text-strong);
  margin-bottom: 16px;
}
.post-comments-title::before {
  content: "";
  width: 5px;
  height: 22px;
  border-radius: 3px;
  background: var(--theme-accent-line-vertical);
}
.post-comment-card {
  background: rgba(8, 3, 5, 0.6);
  border: 1px solid var(--theme-accent-border-soft);
  border-radius: 14px;
  padding: 20px;
}
#tcomment-post {
  color: #ffffff;
}

/* 右侧目录 */
.toc-sidebar-container {
  position: relative;
}
.toc-sidebar {
  position: sticky;
  top: 84px;
  background: var(--theme-accent-panel-soft);
  border: 1px solid var(--theme-accent-border-soft);
  border-radius: 16px;
  padding: 18px 16px;
  max-height: calc(100vh - 110px);
  overflow-y: auto;
  box-shadow: 0 16px 40px var(--theme-accent-shadow);
}
.toc-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  letter-spacing: 0.14em;
  color: var(--theme-accent-text);
  font-weight: 700;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--theme-accent-border-soft);
}
.toc-title::before {
  content: "";
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--theme-accent-strong);
}
.toc-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.toc-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 1px 0;
  border-radius: 7px;
  border-left: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}
.toc-item:hover {
  background: var(--theme-accent-surface-soft);
}
/* 一级（h2）：方块标记、字号略大 */
.toc-item--lvl2 {
  padding: 6px 10px 6px 8px;
}
.toc-item--lvl2 .toc-link {
  font-size: 12.5px;
  font-weight: 600;
  color: #cabdb0;
}
/* 二级及更深（h3+）：缩进 + 树状连接线 */
.toc-item--lvl3,
.toc-item--lvl4,
.toc-item--lvl5,
.toc-item--lvl6 {
  padding: 4px 10px 4px 26px;
}
.toc-item--lvl3 .toc-link,
.toc-item--lvl4 .toc-link,
.toc-item--lvl5 .toc-link,
.toc-item--lvl6 .toc-link {
  font-size: 11.5px;
  color: #a5988f;
}
.toc-item--lvl3::before,
.toc-item--lvl4::before,
.toc-item--lvl5::before,
.toc-item--lvl6::before {
  content: "";
  position: absolute;
  left: 14px;
  top: 50%;
  width: 8px;
  height: 1px;
  background: var(--theme-accent-border);
}
.toc-item--active {
  background: var(--theme-accent-surface);
  border-left-color: var(--theme-accent-strong);
}
.toc-item--active .toc-link {
  color: var(--theme-accent-text-strong);
  font-weight: 600;
}
.toc-toggle {
  font-size: 9px;
  color: #bbb;
  user-select: none;
  width: 10px;
  text-align: center;
  flex: 0 0 auto;
}
.toc-toggle-placeholder {
  visibility: hidden;
}
.toc-link {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 悬浮操作球 */
.fabs {
  position: fixed;
  right: 30px;
  bottom: 36px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 1000;
}
.fab {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid var(--theme-accent-border-soft);
  background: linear-gradient(135deg, rgba(74, 18, 29, 0.95), rgba(214, 173, 92, 0.32));
  color: var(--theme-accent-text-soft);
  cursor: pointer;
  display: grid;
  place-items: center;
  box-shadow: 0 8px 20px var(--theme-accent-shadow-strong);
  transition: all 0.18s ease;
}
.fab svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.8;
}
.fab:hover {
  transform: translateY(-3px);
  color: #fff;
  border-color: var(--theme-accent-border);
  box-shadow: 0 12px 26px var(--theme-accent-shadow-strong), 0 0 0 1px var(--theme-accent-glow);
}
.fab--toc {
  display: none;
}

/* 响应式 */
@media (max-width: 900px) {
  .post-container {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px 14px 60px;
  }
  .post-hero {
    padding: 28px 22px 22px;
  }
  .post-content {
    padding: 26px 22px 4px;
    font-size: 16px;
  }
  .post-footer,
  .post-nav,
  .post-comments {
    margin-left: 22px;
    margin-right: 22px;
  }
  .post-nav {
    grid-template-columns: 1fr;
  }

  /* 目录改为抽屉 */
  .toc-sidebar-container {
    position: static;
  }
  .toc-sidebar {
    position: fixed;
    top: auto;
    right: 16px;
    bottom: 70px;
    width: 72vw;
    max-width: 320px;
    max-height: 70vh;
    z-index: 1100;
    opacity: 0;
    transform: translateY(10px);
    pointer-events: none;
    transition: opacity 0.2s ease, transform 0.2s ease;
  }
  .toc-sidebar--mobile-open {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }
  .fab--toc {
    display: grid;
  }
}
</style>
