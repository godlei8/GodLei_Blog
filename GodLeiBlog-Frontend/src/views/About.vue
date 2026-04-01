<template>
  <div class="about-page">
    <div class="about-inner">
      <section class="hero">
        <div class="hero-top">
          <img
            :src="profileAvatar"
            alt="头像"
            class="hero-avatar"
            @error="handleImageError($event, defaultAvatar)"
          />
          <div class="hero-titles">
            <h1 ref="animatedTitle" class="hero-title"></h1>
            <h2 ref="animatedSubtitle" class="hero-subtitle"></h2>
          </div>
        </div>

        <div class="tags-container" aria-label="标签列表">
          <span
            v-for="tag in tagsWithStyle"
            :key="tag.text"
            class="about-tag"
            :style="{ background: tag.bg }"
          >
            {{ tag.text }}
          </span>
        </div>
      </section>

      <section class="section-card">
        <h2 class="section-title">个人简介</h2>
        <ul class="info-list">
          <li><strong>很高兴认识你</strong></li>
          <li>这里是 GodLei 的博客，主要记录技术、生活和持续折腾的过程。</li>
          <li>喜欢把踩坑、解决思路和灵感都整理下来，也方便以后回头看。</li>
          <li>
            <strong>技术栈：</strong>
            <span class="accent-text">
              Java、Spring Boot、MySQL、Vue、Vite、Git、Docker
            </span>
          </li>
          <li>
            <strong>正在补齐：</strong>
            <span class="accent-text">
              JavaScript、前端工程化、Linux、Nginx、部署与自动化
            </span>
          </li>
          <li>如果这里的内容刚好帮到了你，那这份记录就很值得继续写下去。</li>
        </ul>
      </section>

      <section class="section-card">
        <h2 class="section-title">我的一些社交账号</h2>
        <div class="social-grid">
          <a
            v-for="item in socialLinks"
            :key="`${item.name}-${item.url}`"
            :href="item.url"
            target="_blank"
            rel="noopener"
            class="social-button"
          >
            <i :class="item.icon"></i>
            <span>{{ item.name }}</span>
          </a>
        </div>
      </section>

      <section class="section-card">
        <h2 class="section-title">我的技术栈</h2>
        <div class="tech-marquee" aria-label="技术栈图标滚动">
          <div class="tech-marquee-row tech-marquee-row--left">
            <img
              v-for="item in techIcons"
              :key="item.src"
              :src="item.src"
              :alt="item.name"
              :title="item.name"
              class="tech-icon"
              loading="lazy"
              @error="techIconError"
            />
            <img
              v-for="item in techIcons"
              :key="`dup-${item.src}`"
              :src="item.src"
              :alt="item.name"
              :title="item.name"
              class="tech-icon"
              loading="lazy"
              @error="techIconError"
            />
          </div>

          <div class="tech-marquee-row tech-marquee-row--right">
            <img
              v-for="item in techIcons"
              :key="`${item.src}-right`"
              :src="item.src"
              :alt="item.name"
              :title="item.name"
              class="tech-icon"
              loading="lazy"
              @error="techIconError"
            />
            <img
              v-for="item in techIcons"
              :key="`dup-${item.src}-right`"
              :src="item.src"
              :alt="item.name"
              :title="item.name"
              class="tech-icon"
              loading="lazy"
              @error="techIconError"
            />
          </div>
        </div>
      </section>

      <section class="section-card">
        <div class="anime-title-row">
          <h2 class="section-title">追番列表</h2>
          <button
            v-if="hasAnimeImages"
            type="button"
            class="anime-collapse-btn"
            :aria-expanded="!animeCollapsed"
            @click="toggleAnimeCollapse"
          >
            {{ animeCollapsed ? '展开全部' : '折叠' }}
          </button>
        </div>

        <div v-if="hasAnimeImages" ref="animeGrid" class="anime-grid" aria-label="追番卡片列表">
          <div
            v-for="(image, index) in animeImages"
            :key="`${image}-${index}`"
            v-show="isAnimeCardVisible(index)"
            class="anime-card"
            :title="`番剧 ${index + 1}`"
          >
            <img
              :src="resolveAnimeImage(image, index)"
              :alt="`番剧 ${index + 1}`"
              class="anime-card-img"
              :loading="getAnimeImageLoading(index)"
              @error="handleImageError($event, animeFallback(index))"
            />
          </div>
        </div>

        <div v-else class="empty-state">
          还没有配置追番封面，前往后台站点配置即可添加。
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { applyImageFallback, avatarFallbackUrl, createAnimePosterFallback, resolveImageUrl } from '@/utils/image'
import { getDefaultSiteConfig, loadSiteConfig } from '@/utils/siteConfig'

const tags = [
  '#博客作者',
  '#持续学习',
  '#热衷折腾',
  '#Java / Vue / Spring Boot',
  '#记录技术和生活',
  '#把踩坑写成经验'
]

const gradientPool = [
  'linear-gradient(135deg, #3a1118, #7a1d2d)',
  'linear-gradient(135deg, #4a121d, #9a4e40)',
  'linear-gradient(135deg, #2c0d13, #6b1b2a)',
  'linear-gradient(135deg, #4e1e29, #b98258)',
  'linear-gradient(135deg, #5c1020, #c9963f)',
  'linear-gradient(135deg, #2a0d11, #70424a)',
  'linear-gradient(135deg, #55202c, #e7c777)',
  'linear-gradient(135deg, #341018, #8e4a3a)',
  'linear-gradient(135deg, #43212a, #b38b77)',
  'linear-gradient(135deg, #47151f, #d6ad5c)',
  'linear-gradient(135deg, #301018, #83515c)',
  'linear-gradient(135deg, #5a2330, #f1d39c)'
]

const techIconFiles = [
  'java-original.svg',
  'python-original.svg',
  'html5-original.svg',
  'css3-original.svg',
  'javascript-original.svg',
  'cplusplus-original.svg',
  'vuejs-original.svg',
  'git-original.svg',
  'intellij-original.svg',
  'vscode-original.svg',
  'pycharm-original.svg',
  'astro-original.svg',
  'mysql-original.svg',
  'maven-original.svg',
  'spring-original.svg',
  'uniapp.png'
]

export default {
  name: 'About',
  data() {
    return {
      siteConfig: getDefaultSiteConfig(),
      defaultAvatar: avatarFallbackUrl,
      titleText: '我叫 GodLei',
      subtitleText: '很高兴认识你 Hi~',
      tagsWithStyle: [],
      animeCollapsed: true,
      animeRowsToShow: 2,
      animeVisibleCount: 0,
      animeCardMinWidth: 130,
      animeGridGap: 6
    }
  },
  computed: {
    profileAvatar() {
      return resolveImageUrl(this.siteConfig.basic?.profileAvatar, this.defaultAvatar)
    },
    socialLinks() {
      const defaults = getDefaultSiteConfig().home.socialLinks
      return this.siteConfig.home?.socialLinks?.length ? this.siteConfig.home.socialLinks : defaults
    },
    techIcons() {
      return techIconFiles
        .map((file) => ({
          src: this.getTechIconSrc(file),
          name: this.getTechIconName(file)
        }))
        .filter((item) => item.src)
    },
    techIconFallback() {
      const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64"><rect x="6" y="6" width="52" height="52" rx="14" fill="rgba(74,18,29,0.16)" stroke="rgba(214,173,92,0.32)" /><text x="50%" y="56%" dominant-baseline="middle" text-anchor="middle" font-family="Arial, sans-serif" font-size="28" fill="#e7c777">?</text></svg>'
      return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
    },
    githubStatsCard() {
      return 'https://github-readme-stats.vercel.app/api?username=godlei&show_icons=true&hide_border=true&bg_color=00000000&title_color=d6ad5c&text_color=fff7ea&icon_color=e7c777'
    },
    githubLangsCard() {
      return 'https://github-readme-stats.vercel.app/api/top-langs/?username=godlei&layout=compact&hide_border=true&bg_color=00000000&title_color=d6ad5c&text_color=fff7ea'
    },
    animeImages() {
      return Array.isArray(this.siteConfig.about?.animeImages) ? this.siteConfig.about.animeImages : []
    },
    hasAnimeImages() {
      return this.animeImages.length > 0
    }
  },
  async mounted() {
    this.tagsWithStyle = tags.map((text) => ({
      text,
      bg: gradientPool[Math.floor(Math.random() * gradientPool.length)]
    }))

    this.initAnimatedTitles()
    await this.refreshConfig(true)
    this.$nextTick(() => {
      this.updateAnimeVisibleCount()
    })
    window.addEventListener('resize', this.onAnimeResize)
    window.addEventListener('focus', this.handleWindowFocus)
    document.addEventListener('visibilitychange', this.handleVisibilityChange)
  },
  beforeUnmount() {
    clearTimeout(this._animeResizeTimer)
    window.removeEventListener('resize', this.onAnimeResize)
    window.removeEventListener('focus', this.handleWindowFocus)
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)
  },
  methods: {
    async refreshConfig(force = false) {
      this.siteConfig = await loadSiteConfig(force)
      await this.$nextTick()
      this.updateAnimeVisibleCount()
    },
    handleWindowFocus() {
      this.refreshConfig(true).catch((error) => {
        console.error('刷新站点配置失败:', error)
      })
    },
    handleVisibilityChange() {
      if (document.hidden) return
      this.refreshConfig(true).catch((error) => {
        console.error('刷新站点配置失败:', error)
      })
    },
    getTechIconSrc(file) {
      switch (file) {
        case 'java-original.svg':
          return new URL('../assets/imgs/icon/java-original.svg', import.meta.url).href
        case 'python-original.svg':
          return new URL('../assets/imgs/icon/python-original.svg', import.meta.url).href
        case 'html5-original.svg':
          return new URL('../assets/imgs/icon/html5-original.svg', import.meta.url).href
        case 'css3-original.svg':
          return new URL('../assets/imgs/icon/css3-original.svg', import.meta.url).href
        case 'javascript-original.svg':
          return new URL('../assets/imgs/icon/javascript-original.svg', import.meta.url).href
        case 'cplusplus-original.svg':
          return new URL('../assets/imgs/icon/cplusplus-original.svg', import.meta.url).href
        case 'vuejs-original.svg':
          return new URL('../assets/imgs/icon/vuejs-original.svg', import.meta.url).href
        case 'git-original.svg':
          return new URL('../assets/imgs/icon/git-original.svg', import.meta.url).href
        case 'intellij-original.svg':
          return new URL('../assets/imgs/icon/intellij-original.svg', import.meta.url).href
        case 'vscode-original.svg':
          return new URL('../assets/imgs/icon/vscode-original.svg', import.meta.url).href
        case 'pycharm-original.svg':
          return new URL('../assets/imgs/icon/pycharm-original.svg', import.meta.url).href
        case 'astro-original.svg':
          return new URL('../assets/imgs/icon/astro-original.svg', import.meta.url).href
        case 'mysql-original.svg':
          return new URL('../assets/imgs/icon/mysql-original.svg', import.meta.url).href
        case 'maven-original.svg':
          return new URL('../assets/imgs/icon/maven-original.svg', import.meta.url).href
        case 'spring-original.svg':
          return new URL('../assets/imgs/icon/spring-original.svg', import.meta.url).href
        case 'uniapp.png':
          return new URL('../assets/imgs/icon/uniapp.png', import.meta.url).href
        default:
          return ''
      }
    },
    getTechIconName(file) {
      switch (file) {
        case 'java-original.svg':
          return 'Java'
        case 'python-original.svg':
          return 'Python'
        case 'html5-original.svg':
          return 'HTML5'
        case 'css3-original.svg':
          return 'CSS3'
        case 'javascript-original.svg':
          return 'JavaScript'
        case 'cplusplus-original.svg':
          return 'C++'
        case 'vuejs-original.svg':
          return 'Vue'
        case 'git-original.svg':
          return 'Git'
        case 'intellij-original.svg':
          return 'IntelliJ IDEA'
        case 'vscode-original.svg':
          return 'VS Code'
        case 'pycharm-original.svg':
          return 'PyCharm'
        case 'astro-original.svg':
          return 'Astro'
        case 'mysql-original.svg':
          return 'MySQL'
        case 'maven-original.svg':
          return 'Maven'
        case 'spring-original.svg':
          return 'Spring'
        case 'uniapp.png':
          return 'UniApp'
        default:
          return file
      }
    },
    initAnimatedTitles() {
      const titleEl = this.$refs.animatedTitle
      const subtitleEl = this.$refs.animatedSubtitle
      if (!titleEl || !subtitleEl) return

      const buildChars = (el, text, className) => {
        el.innerHTML = ''
        for (let index = 0; index < text.length; index += 1) {
          const span = document.createElement('span')
          span.className = className
          span.textContent = text[index] === ' ' ? '\u00A0' : text[index]
          span.style.animationDelay = `${index * 0.08}s`
          el.appendChild(span)
        }
      }

      buildChars(titleEl, this.titleText, 'title-char')
      buildChars(subtitleEl, this.subtitleText, 'subtitle-char')
    },
    onAnimeResize() {
      clearTimeout(this._animeResizeTimer)
      this._animeResizeTimer = setTimeout(() => {
        this.updateAnimeVisibleCount()
      }, 150)
    },
    updateAnimeVisibleCount() {
      if (!this.hasAnimeImages) {
        this.animeVisibleCount = 0
        return
      }

      const gridEl = this.$refs.animeGrid
      let columnCount = 0

      if (gridEl && typeof window !== 'undefined') {
        const style = window.getComputedStyle(gridEl)
        const columns = style?.gridTemplateColumns

        if (columns && columns.includes('px')) {
          columnCount = columns.split(' ').filter(Boolean).length
        }

        if (!columnCount) {
          const width = gridEl.clientWidth || 0
          columnCount = Math.max(1, Math.floor((width + this.animeGridGap) / (this.animeCardMinWidth + this.animeGridGap)))
        }
      }

      columnCount = Math.max(1, columnCount || 1)
      this.animeVisibleCount = Math.min(columnCount * this.animeRowsToShow, this.animeImages.length)
    },
    getDefaultAnimeColumnCount() {
      if (typeof window === 'undefined') return 5
      if (window.innerWidth <= 520) return 2
      if (window.innerWidth <= 760) return 3
      if (window.innerWidth <= 980) return 4
      return 5
    },
    getCollapsedAnimeLimit() {
      if (this.animeVisibleCount > 0) return this.animeVisibleCount
      return Math.min(this.getDefaultAnimeColumnCount() * this.animeRowsToShow, this.animeImages.length)
    },
    isAnimeCardVisible(index) {
      if (!this.animeCollapsed) return true
      return index < this.getCollapsedAnimeLimit()
    },
    getAnimeImageLoading(index) {
      return this.isAnimeCardVisible(index) ? 'eager' : 'lazy'
    },
    toggleAnimeCollapse() {
      this.animeCollapsed = !this.animeCollapsed
      this.$nextTick(() => {
        this.updateAnimeVisibleCount()
      })
    },
    resolveAnimeImage(url, index) {
      return resolveImageUrl(url, this.animeFallback(index))
    },
    animeFallback(index) {
      return createAnimePosterFallback(index)
    },
    hideImage(event) {
      const target = event?.target
      if (target) target.style.display = 'none'
    },
    techIconError(event) {
      const target = event?.target
      if (!target) return
      if (target.src !== this.techIconFallback) {
        target.src = this.techIconFallback
        return
      }
      target.style.display = 'block'
    },
    handleImageError(event, fallback) {
      applyImageFallback(event, fallback)
    }
  }
}
</script>

<style scoped>
.about-page {
  width: 100%;
  min-height: calc(100vh - 68px);
  padding: 20px;
  box-sizing: border-box;
  color: #fff;
  display: flex;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
}

.about-inner {
  width: 100%;
  max-width: 1100px;
}

.hero {
  padding: 22px 18px;
  border-radius: 16px;
  border: 1px solid var(--theme-accent-border-soft);
  background: var(--theme-accent-panel);
  box-shadow: 0 10px 30px var(--theme-accent-shadow);
}

.hero-top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-avatar {
  width: 110px;
  height: 110px;
  border-radius: 18px;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 0 0 6px rgba(214, 173, 92, 0.12);
}

.hero-titles {
  flex: 1;
  min-width: 0;
}

.hero-title {
  margin: 0;
  font-family: 'Bebas Neue', Arial, sans-serif;
  font-size: 46px;
  line-height: 1.05;
  letter-spacing: 0.06em;
  color: var(--theme-accent-text-strong);
  text-shadow: 0 0 18px rgba(214, 173, 92, 0.18);
}

.hero-subtitle {
  margin: 10px 0 0;
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

:deep(.title-char),
:deep(.subtitle-char) {
  display: inline-block;
  opacity: 0;
  color: inherit;
  animation: titleCharIn 0.65s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

:deep(.title-char) {
  color: var(--theme-accent-text-strong);
}

:deep(.subtitle-char) {
  animation-name: subtitleCharIn;
}

@supports ((-webkit-background-clip: text) or (background-clip: text)) {
  :deep(.title-char) {
    background-image: var(--theme-accent-title-gradient);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    color: transparent;
    text-shadow: none;
  }
}

@keyframes titleCharIn {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.45);
    filter: blur(1px);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes subtitleCharIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.6);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.tags-container {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.about-tag {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  border: 1px solid var(--theme-accent-border-soft);
  box-shadow: inset 0 0 12px rgba(255, 255, 255, 0.08);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}

.section-card {
  margin-top: 18px;
  padding: 20px 18px;
  border-radius: 16px;
  border: 1px solid var(--theme-accent-border-soft);
  background: var(--theme-accent-panel-soft);
  box-shadow: 0 10px 26px var(--theme-accent-shadow);
}

.section-title {
  margin: 0 0 12px;
  font-family: 'Bebas Neue', Arial, sans-serif;
  font-size: 34px;
  letter-spacing: 0.09em;
  background-image: var(--theme-accent-title-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.stats-inline {
  margin-top: 14px;
}

.section-title--stats {
  margin-bottom: 8px;
  font-size: 22px;
}

.stats-images {
  display: flex;
  gap: 14px;
}

.stats-images.stats-images--stats {
  display: grid;
  grid-template-columns: max-content;
  row-gap: 8px;
  column-gap: 12px;
  align-items: start;
  justify-items: start;
  justify-content: flex-start;
  width: 100%;
}

.stats-img {
  margin: 0 auto;
  border-radius: 12px;
  border: 1px solid var(--theme-accent-border-soft);
  background: rgba(255, 255, 255, 0.03);
}

.stats-img--stats {
  max-width: 320px;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
}

.stats-img--stats-first {
  max-width: 180px;
}

.info-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.info-list li {
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.92);
  padding-left: 14px;
  position: relative;
}

.info-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.6em;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--theme-accent-text);
  box-shadow: 0 0 12px var(--theme-accent-glow);
}

.accent-text {
  color: var(--theme-accent-text);
  font-weight: 600;
}

.social-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.social-button {
  text-decoration: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 12px;
  border-radius: 14px;
  border: 1px solid var(--theme-accent-border-soft);
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 10px 22px var(--theme-accent-shadow-soft);
  color: #fff;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border 0.15s ease, background 0.15s ease;
}

.social-button i {
  font-size: 18px;
  color: var(--theme-accent-text);
}

.social-button span {
  font-weight: 700;
  letter-spacing: 0.02em;
}

.social-button:hover {
  transform: translateY(-2px);
  border: 1px solid var(--theme-accent-border-strong);
  background: var(--theme-accent-surface);
  box-shadow: 0 16px 34px var(--theme-accent-shadow);
}

.tech-marquee {
  --tech-marquee-bg: rgba(255, 255, 255, 0.07);
  --tech-marquee-border: var(--theme-accent-border-soft);
  --tech-icon-bg: rgba(122, 29, 45, 0.18);
  --tech-icon-border: rgba(214, 173, 92, 0.34);
  --tech-icon-shadow: rgba(12, 3, 5, 0.34);

  border-radius: 10px;
  border: 1px solid var(--tech-marquee-border);
  background: var(--tech-marquee-bg);
  overflow: hidden;
  padding: 22px 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tech-marquee-row {
  width: max-content;
  display: flex;
  gap: 22px;
  padding: 0 18px;
  align-items: center;
  animation: techMarquee 55s linear infinite;
}

@keyframes techMarquee {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

.tech-marquee-row--right {
  animation: techMarqueeReverse 55s linear infinite;
}

@keyframes techMarqueeReverse {
  from {
    transform: translateX(-50%);
  }
  to {
    transform: translateX(0);
  }
}

.tech-icon {
  width: 106px;
  height: 106px;
  display: block;
  box-sizing: border-box;
  padding: 14px;
  border-radius: 16px;
  background: var(--tech-icon-bg);
  border: 1px solid var(--tech-icon-border);
  object-fit: contain;
  opacity: 0.95;
  filter: drop-shadow(0 8px 20px var(--tech-icon-shadow));
}

.anime-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.anime-title-row .section-title {
  margin: 0;
}

.anime-collapse-btn {
  flex: 0 0 auto;
  background: var(--theme-accent-panel-button);
  border: 1px solid var(--theme-accent-border);
  color: var(--theme-accent-text-strong);
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
}

.anime-collapse-btn:hover {
  border-color: var(--theme-accent-border-strong);
  box-shadow: 0 10px 24px var(--theme-accent-shadow);
}

.anime-collapse-btn:focus {
  outline: none;
  box-shadow: 0 0 0 3px var(--theme-accent-glow-soft);
}

.anime-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  padding: 0;
}

.anime-card {
  position: relative;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.35);
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.18);
  transition: transform 0.15s ease, border 0.15s ease, box-shadow 0.15s ease;
}

.anime-card::before {
  content: '';
  position: absolute;
  left: -20%;
  top: -30%;
  width: 140%;
  height: 55px;
  background: var(--theme-accent-scanline);
  transform: translateY(-60px);
  opacity: 0;
  transition: opacity 0.15s ease, transform 0.25s ease;
  pointer-events: none;
}

.anime-card:hover {
  transform: translateY(-3px);
  border-color: var(--theme-accent-border-strong);
  box-shadow: 0 18px 40px var(--theme-accent-shadow);
}

.anime-card:hover::before {
  opacity: 1;
  transform: translateY(160px);
}

.anime-card-img {
  width: 100%;
  aspect-ratio: 125 / 176;
  height: auto;
  object-fit: cover;
  display: block;
  filter: grayscale(30%);
  transition: filter 0.2s ease, transform 0.2s ease;
  background: transparent;
}

.anime-card:hover .anime-card-img {
  filter: grayscale(0%);
  transform: scale(1.02);
}

.empty-state {
  margin-top: 6px;
  padding: 16px;
  border-radius: 16px;
  border: 1px dashed var(--theme-accent-border-soft);
  color: var(--theme-accent-text-soft);
  background: rgba(255, 255, 255, 0.03);
  line-height: 1.7;
}

@media (max-width: 980px) {
  .hero-title {
    font-size: 42px;
  }

  .anime-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .anime-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .about-page {
    padding: 14px;
  }

  .hero-top {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .hero-avatar {
    width: 96px;
    height: 96px;
  }

  .hero-title {
    font-size: 34px;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .social-grid {
    grid-template-columns: 1fr 1fr;
  }

  .section-card {
    padding: 18px 14px;
  }

  .anime-title-row {
    margin-bottom: 10px;
  }

  .anime-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
