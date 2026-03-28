<template>
  <LoadingSpinner v-if="showLoader" @animation-finished="onAnimationFinished" />

  <div v-else class="home-shell">
    <section class="home">
      <div class="left-top-line"></div>
      <div class="left-top-line2"></div>
      <div class="left-top-line3"></div>
      <div class="left-top-line4"></div>
      <div class="left-top-line5"></div>

      <div class="welcome-banner">
        <div ref="line1" class="line1"></div>
        <div ref="line2" class="line2"></div>
      </div>

      <div class="content">
        <p
          v-for="(line, index) in introLines"
          :key="`intro-${index}`"
          class="fade-in-text"
        >
          {{ line }}
        </p>
        <div class="left-bottom-line6"></div>
      </div>

      <div class="blank"></div>
    </section>

    <button class="arrow-container" type="button" @click="scrollToPosts" aria-label="滚动到文章列表">
      <div class="guide-line"></div>
      <div class="arrow-down"></div>
    </button>

    <div class="card-container">
      <div
        class="card row-reveal-item"
        :class="{ 'row-revealed': isHomeRowRevealed('notice-row') }"
        data-row-key="notice-row"
      >
        <img
          id="home-card-avatar"
          :src="profileAvatar"
          alt="头像"
          @error="handleImageError($event, defaultAvatar)"
        />

        <div class="card-content">
          <p class="card-title">{{ siteConfig.home.noticeTitle }}</p>
          <p
            v-for="(line, index) in noticeLines"
            :key="`notice-${index}`"
            class="card-line"
          >
            {{ line }}
          </p>

          <div class="social-icons">
            <a
              v-for="item in socialLinks"
              :key="`${item.name}-${item.url}`"
              :href="item.url"
              target="_blank"
              rel="noopener"
              class="icon"
              :title="item.name"
            >
              <i :class="item.icon"></i>
            </a>
          </div>
        </div>
      </div>

      <div id="posts" class="posts-container">
        <div
          v-for="(post, index) in posts"
          :key="post.id"
          class="post-card row-reveal-item"
          :data-post-id="post.id"
          :data-row-key="getPostRowKey(index)"
          :class="[
            {
              'scan-active': isActive(post.id) || hoveredCardId === post.id,
              'active-post': isActive(post.id),
              'row-revealed': isHomeRowRevealed(getPostRowKey(index))
            }
          ]"
          @mouseenter="hoveredCardId = post.id"
          @mouseleave="hoveredCardId = null"
          @click="goToPost(post.id)"
        >
          <img
            :src="resolvePostCover(post.cover)"
            :alt="post.title"
            class="post-cover"
            @error="handleImageError($event, defaultCover)"
          />
          <div class="post-info">
            <h3 class="post-title">{{ post.title }}</h3>
            <p class="post-description">{{ truncateContent(post.description) }}</p>
            <p class="post-date">更新于 {{ formatDate(post.updateTime || post.createTime) }}</p>
          </div>
        </div>
      </div>

      <div v-if="!posts.length" class="empty-state">暂无文章，晚点再来看看。</div>

      <div v-if="totalPages > 1" class="pagination">
        <button type="button" @click="prevPage" :disabled="currentPage === 1">上一页</button>
        <span>{{ currentPage }} / {{ totalPages }}</span>
        <button type="button" @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
      </div>
    </div>
  </div>
</template>

<script>
import { fetchPosts } from '@/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { applyImageFallback, avatarFallbackUrl, coverFallbackUrl, resolveImageUrl } from '@/utils/image'
import { getDefaultSiteConfig, loadSiteConfig } from '@/utils/siteConfig'

export default {
  name: 'Home',
  components: {
    LoadingSpinner
  },
  data() {
    return {
      posts: [],
      currentPage: 1,
      pageSize: 12,
      total: 0,
      defaultAvatar: avatarFallbackUrl,
      defaultCover: coverFallbackUrl,
      siteConfig: getDefaultSiteConfig(),
      hoveredCardId: null,
      activePostIds: [],
      revealedRowKeys: [],
      cardObserver: null,
      resizeTimer: null,
      showLoader: true,
      bootAnimationFinished: false,
      contentReady: false,
      homeInitialized: false
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    profileAvatar() {
      return resolveImageUrl(this.siteConfig.basic.profileAvatar, this.defaultAvatar)
    },
    backgroundImageUrl() {
      return resolveImageUrl(this.siteConfig.home.backgroundImage, this.defaultCover)
    },
    socialLinks() {
      return this.siteConfig.home.socialLinks?.length ? this.siteConfig.home.socialLinks : getDefaultSiteConfig().home.socialLinks
    },
    introLines() {
      return this.siteConfig.home.introLines?.length ? this.siteConfig.home.introLines : getDefaultSiteConfig().home.introLines
    },
    noticeLines() {
      return this.siteConfig.home.noticeLines?.length ? this.siteConfig.home.noticeLines : getDefaultSiteConfig().home.noticeLines
    }
  },
  async mounted() {
    try {
      const [siteConfig] = await Promise.all([
        loadSiteConfig(true),
        this.loadPosts()
      ])
      this.siteConfig = siteConfig
      this.applyHomeBackground()
    } finally {
      this.contentReady = true
      if (this.bootAnimationFinished) {
        this.showLoader = false
        this.$nextTick(() => {
          this.initializeHomeView()
        })
      }
    }
    window.addEventListener('focus', this.handleWindowFocus)
    document.addEventListener('visibilitychange', this.handleVisibilityChange)
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.updateActivePosts)
    window.removeEventListener('resize', this.handleHomeResize)
    window.removeEventListener('focus', this.handleWindowFocus)
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)
    if (this.cardObserver) {
      this.cardObserver.disconnect()
      this.cardObserver = null
    }
    if (this.resizeTimer) {
      clearTimeout(this.resizeTimer)
      this.resizeTimer = null
    }
  },
  methods: {
    async refreshSiteConfig(force = false) {
      const previousPrefix = this.siteConfig.home?.welcomePrefix
      const previousHighlight = this.siteConfig.home?.welcomeHighlight
      this.siteConfig = await loadSiteConfig(force)
      this.applyHomeBackground()
      const titleChanged =
        previousPrefix !== this.siteConfig.home?.welcomePrefix
        || previousHighlight !== this.siteConfig.home?.welcomeHighlight
      if (titleChanged && this.$refs.line1 && this.$refs.line2) {
        this.animateText()
      }
    },
    handleWindowFocus() {
      this.refreshSiteConfig(true).catch((error) => {
        console.error('刷新站点配置失败:', error)
      })
    },
    handleVisibilityChange() {
      if (document.hidden) return
      this.refreshSiteConfig(true).catch((error) => {
        console.error('刷新站点配置失败:', error)
      })
    },
    onAnimationFinished() {
      this.bootAnimationFinished = true
      if (this.contentReady) {
        this.showLoader = false
        this.$nextTick(() => {
          this.initializeHomeView()
        })
      }
    },
    initializeHomeView() {
      if (this.homeInitialized) return
      this.homeInitialized = true
      this.animateText()
      window.addEventListener('scroll', this.updateActivePosts, { passive: true })
      window.addEventListener('resize', this.handleHomeResize, { passive: true })
      this.updateActivePosts()
      this.setupHomeRowReveal()
    },
    applyHomeBackground() {
      document.documentElement.style.setProperty('--site-home-bg-image', `url("${this.backgroundImageUrl}")`)
    },
    async loadPosts() {
      try {
        const response = await fetchPosts(this.currentPage, this.pageSize)
        this.posts = response?.data?.rows || []
        this.total = response?.data?.total || 0
        this.$nextTick(() => {
          this.revealedRowKeys = []
          if (this.homeInitialized) {
            this.setupHomeRowReveal()
          }
        })
      } catch (error) {
        console.error('加载文章失败:', error)
        this.posts = []
        this.total = 0
      }
    },
    goToPost(postId) {
      this.$router.push({ name: 'PostDetail', params: { id: postId } })
    },
    scrollToPosts() {
      const target = document.getElementById('posts')
      if (!target) return
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    resolvePostCover(url) {
      return resolveImageUrl(url, this.defaultCover)
    },
    handleImageError(event, fallback) {
      applyImageFallback(event, fallback)
    },
    formatDate(value) {
      if (!value) return '暂无日期'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '暂无日期'
      return date.toLocaleDateString('zh-CN')
    },
    truncateContent(description) {
      if (!description) return '暂无描述'
      return description.length > 90 ? `${description.slice(0, 90)}...` : description
    },
    async prevPage() {
      if (this.currentPage === 1) return
      this.currentPage -= 1
      await this.loadPosts()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    async nextPage() {
      if (this.currentPage >= this.totalPages) return
      this.currentPage += 1
      await this.loadPosts()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    isMobileView() {
      return window.innerWidth <= 768
    },
    updateActivePosts() {
      if (!this.isMobileView()) {
        this.activePostIds = []
        return
      }

      const scrollY = window.scrollY
      const viewportHeight = window.innerHeight
      if (scrollY < viewportHeight * 0.7) {
        this.activePostIds = []
        return
      }

      const cards = document.querySelectorAll('.posts-container .post-card')
      if (!cards.length) {
        this.activePostIds = []
        return
      }

      const viewportCenterY = viewportHeight / 2
      const distances = []
      cards.forEach((card) => {
        const rect = card.getBoundingClientRect()
        const cardCenterY = rect.top + rect.height / 2
        const distance = Math.abs(cardCenterY - viewportCenterY)
        const idAttr = card.getAttribute('data-post-id')
        if (idAttr) {
          distances.push({ id: idAttr, distance })
        }
      })

      distances.sort((a, b) => a.distance - b.distance)
      const nearestIds = distances.map((item) => item.id)
      const active = []
      if (nearestIds.length > 0) {
        active.push(nearestIds[0])
      }
      if (scrollY >= viewportHeight && nearestIds.length > 1) {
        active.push(nearestIds[1])
      }

      this.activePostIds = active
    },
    isActive(postId) {
      return this.activePostIds.includes(String(postId))
    },
    handleHomeResize() {
      if (this.resizeTimer) clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        this.revealedRowKeys = []
        this.animateText()
        this.setupHomeRowReveal()
      }, 140)
    },
    getPostColumns() {
      return window.innerWidth <= 768 ? 1 : 3
    },
    getPostRowKey(index) {
      return `post-row-${Math.floor(index / this.getPostColumns())}`
    },
    isHomeRowRevealed(rowKey) {
      return this.revealedRowKeys.includes(rowKey)
    },
    setupHomeRowReveal() {
      if (typeof window === 'undefined' || !('IntersectionObserver' in window)) return
      if (this.cardObserver) this.cardObserver.disconnect()

      this.cardObserver = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          const rowKey = entry.target.getAttribute('data-row-key')
          if (rowKey && !this.revealedRowKeys.includes(rowKey)) {
            this.revealedRowKeys.push(rowKey)
          }
          this.cardObserver.unobserve(entry.target)
        })
      }, {
        threshold: 0.15,
        rootMargin: '0px 0px -8% 0px'
      })

      const noticeCard = document.querySelector('.card-container .card.row-reveal-item')
      if (noticeCard) this.cardObserver.observe(noticeCard)

      const postCards = document.querySelectorAll('.posts-container .post-card.row-reveal-item')
      const columns = this.getPostColumns()
      for (let index = 0; index < postCards.length; index += columns) {
        this.cardObserver.observe(postCards[index])
      }
    },
    animateText() {
      const line1 = this.siteConfig.home.welcomePrefix || 'WELCOME TO'
      const line2 = this.siteConfig.home.welcomeHighlight || 'GODLEI BLOG'
      const isMobile = window.innerWidth <= 768

      if (!this.$refs.line1 || !this.$refs.line2) return

      if (isMobile) {
        this.$refs.line1.innerHTML = this.wrapCharacters('WELCOME')
        const mobileTo = this.wrapCharacters('TO', 'mobile-to-char', 0.5)
        this.$refs.line2.innerHTML =
          `<div class="mobile-line-to">${mobileTo}</div>` +
          `<div class="mobile-line-title">${String(line2).replace(/\s+/g, '\n')}</div>`
      } else {
        this.$refs.line1.innerHTML = this.wrapCharacters(line1)
        this.$refs.line2.textContent = line2
      }

      this.triggerAnimation(this.$refs.line1)
    },
    wrapCharacters(text, extraClass = '', startDelay = 0) {
      return String(text)
        .split('')
        .map((char, index) => {
          const className = ['char', extraClass].filter(Boolean).join(' ')
          return `<span class="${className}" style="animation-delay: ${startDelay + index * 0.1}s">${char === ' ' ? '&nbsp;' : char}</span>`
        })
        .join('')
    },
    triggerAnimation(container) {
      const chars = container.querySelectorAll('.char')
      chars.forEach((char, index) => {
        if (!char.style.animationDelay) {
          char.style.animationDelay = `${index * 0.1}s`
        }
      })
    }
  }
}
</script>

<style scoped>
@font-face {
  font-family: 'Bebas Neue';
  src: url('@/assets/fonts/BebasNeue-Regular.ttf') format('truetype');
  font-display: swap;
}

.home-shell {
  position: relative;
}

.home {
  padding: 20px;
  color: white;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 68px);
}

.welcome-banner {
  font-family: 'Bebas Neue', Arial, sans-serif;
  text-align: left;
  color: #fff;
  padding: 100px;
  border-radius: 10px;
}

.line1,
.line2 {
  font-weight: normal;
  text-transform: uppercase;
  letter-spacing: 6px;
  line-height: 1.1;
}

.line1 {
  font-size: 6.5vw;
}

.line2 {
  font-size: 8vw;
  background-image: var(--theme-accent-title-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  white-space: pre-line;
  display: inline-block;
  overflow: hidden;
  opacity: 0;
  transform: translateX(100%);
  animation: slideInLeft 2s cubic-bezier(0.68, -0.55, 0.265, 1.55) forwards;
}

.line1 .char {
  display: inline-block;
  opacity: 0;
  color: var(--theme-accent-text-strong);
  animation: fadeIn 0.5s forwards;
}

@keyframes slideInLeft {
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes fadeIn {
  to {
    opacity: 1;
  }
}

.content {
  flex: 1;
  font-size: 18px;
  line-height: 1.6;
  text-align: left;
  color: rgba(255, 235, 212, 0.74);
  padding: 0 100px;
  border-radius: 10px;
}

.fade-in-text {
  opacity: 0;
  animation: fadeInContent 1s forwards;
  margin: 0 0 4px;
}

.content p:nth-child(1) {
  animation-delay: 0.5s;
}

.content p:nth-child(2) {
  animation-delay: 1s;
}

.content p:nth-child(3) {
  animation-delay: 1.5s;
}

@keyframes fadeInContent {
  to {
    opacity: 1;
  }
}

.left-top-line,
.left-top-line2,
.left-top-line3,
.left-top-line4,
.left-top-line5,
.left-bottom-line6 {
  position: absolute;
  background: var(--theme-accent-text);
}

.left-top-line {
  top: 80px;
  left: 70px;
  width: 130px;
  height: 10px;
}

.left-top-line2 {
  top: 90px;
  left: 70px;
  width: 10px;
  height: 10px;
}

.left-top-line3 {
  top: 110px;
  left: 70px;
  width: 10px;
  height: 5px;
}

.left-top-line4 {
  top: 80px;
  left: 210px;
  width: 20px;
  height: 10px;
}

.left-top-line5 {
  top: 80px;
  left: 250px;
  width: 5px;
  height: 10px;
}

.left-bottom-line6 {
  left: 70px;
  width: 350px;
  height: 7px;
  background: rgba(214, 173, 92, 0.46);
}

.blank {
  height: 60px;
}

.arrow-container {
  position: absolute;
  bottom: 60px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  animation: floatAndBlink 2s infinite ease-in-out;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
}

.guide-line {
  width: 2px;
  height: 40px;
  background-color: rgba(214, 173, 92, 0.72);
  border-radius: 999px;
  box-shadow: rgba(214, 173, 92, 0.48) 0 0 5px;
}

.arrow-down {
  width: 30px;
  height: 30px;
  background-color: var(--theme-accent-arrow);
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z'/%3E%3C/svg%3E") no-repeat center;
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z'/%3E%3C/svg%3E") no-repeat center;
}

@keyframes floatAndBlink {
  0%,
  100% {
    transform: translateX(-50%) translateY(0);
    opacity: 1;
  }

  50% {
    transform: translateX(-50%) translateY(-10px);
    opacity: 0.5;
  }
}

.card-container {
  flex-direction: column;
  align-items: center;
  display: flex;
  justify-content: center;
}

.card {
  display: flex;
  gap: 20px;
  position: relative;
  width: 70%;
  max-width: 1000px;
  background: var(--theme-accent-panel);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 10px;
  text-align: center;
  color: white;
  box-shadow: inset 0 0 10px rgba(255, 255, 255, 0.08), 0 4px 15px rgba(0, 0, 0, 0.3);
}

.card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent);
  border-radius: 10px;
  pointer-events: none;
}

.card.row-reveal-item {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.55s ease, transform 0.55s ease;
}

.card.row-reveal-item.row-revealed {
  opacity: 1;
  transform: translateY(0);
}

#home-card-avatar {
  width: 170px;
  height: 170px;
  border: 3px solid rgba(255, 255, 255, 0.45);
  margin: 20px;
  border-radius: 12px;
  object-fit: cover;
}

.card-content {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  text-align: left;
  padding: 20px 20px 20px 0;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px;
}

.card-line {
  margin: 0 0 6px;
  color: rgba(255, 241, 225, 0.82);
  line-height: 1.6;
}

.social-icons {
  display: flex;
  gap: 15px;
  margin-top: 15px;
}

.icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: var(--theme-accent-text-strong);
  text-decoration: none;
  transition: all 0.3s ease;
}

.icon:hover {
  transform: scale(1.1);
  background: rgba(214, 173, 92, 0.2);
}

.posts-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px 0;
  width: 70%;
  max-width: 1000px;
  box-sizing: border-box;
}

.post-card {
  background: var(--theme-accent-panel-soft);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  border: 2px solid transparent;
  position: relative;
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.55s ease, transform 0.55s ease;
  cursor: pointer;
}

.post-card.row-revealed {
  opacity: 1;
  transform: translateY(0);
}

.post-card:hover {
  transform: translateY(-5px);
  border: 2px solid var(--theme-accent-text);
  box-shadow: 0 6px 15px rgba(214, 173, 92, 0.24);
}

.post-card::before {
  content: '';
  position: absolute;
  top: -100%;
  left: 0;
  width: 100%;
  height: 5px;
  background: var(--theme-accent-scanline);
  z-index: 10;
  transition: none;
}

.post-card.scan-active::before {
  animation: scanLine 1s ease-in-out;
}

@keyframes scanLine {
  0% {
    top: 0%;
  }

  100% {
    top: 100%;
  }
}

.post-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  filter: grayscale(100%);
  transition: filter 0.3s ease, transform 0.3s ease;
}

.post-card:hover .post-cover,
.active-post .post-cover {
  filter: grayscale(0%);
}

.post-info {
  padding: 15px;
}

.post-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 10px;
  color: var(--theme-accent-text-strong);
}

.post-date {
  font-size: 12px;
  color: rgba(255, 235, 212, 0.66);
  margin-bottom: 0;
  text-align: right;
  margin-top: 6px;
}

.post-description {
  font-size: 13px;
  color: rgba(255, 241, 225, 0.72);
  line-height: 1.4;
  margin-bottom: 0;
  min-height: 54px;
}

.empty-state {
  width: 70%;
  max-width: 1000px;
  padding: 18px 16px;
  border-radius: 10px;
  color: rgba(255, 241, 225, 0.82);
  background: rgba(8, 3, 4, 0.6);
  border: 1px solid rgba(214, 173, 92, 0.18);
  text-align: center;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 30px;
}

.pagination button {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(214, 173, 92, 0.18);
  border-radius: 5px;
  color: var(--theme-accent-text-strong);
  cursor: pointer;
}

.pagination button:hover:not(:disabled) {
  background: rgba(214, 173, 92, 0.18);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (min-width: 769px) {
  .home {
    padding-top: 32px;
  }

  .left-top-line {
    top: 122px;
  }

  .left-top-line2 {
    top: 132px;
  }

  .left-top-line3 {
    top: 152px;
  }

  .left-top-line4 {
    top: 122px;
  }

  .left-top-line5 {
    top: 122px;
  }

  .arrow-container {
    transform: translateX(-50%) translateY(12px);
  }

  .card-container {
    margin-top: 40px;
  }
}

@media (max-width: 768px) {
  .home {
    padding: 16px;
  }

  .welcome-banner {
    padding: 72px 20px 20px;
    text-align: left;
  }

  .line1 {
    font-size: 18vw;
    font-weight: 550;
  }

  .line2 {
    font-size: 26vw;
    font-weight: 550;
    color: #fff;
    white-space: normal;
    opacity: 1;
    transform: none;
    animation: none;
  }

  .line2 .mobile-line-to {
    display: block;
  }

  .line2 .mobile-line-to .mobile-to-char {
    display: inline-block;
    opacity: 0;
    color: #fff;
    animation: fadeIn 0.5s forwards;
  }

  .line2 .mobile-line-title {
    opacity: 0;
    display: block;
    white-space: pre-line;
    background-image: var(--theme-accent-title-gradient);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    animation: fadeIn 0.6s forwards;
    animation-delay: 1.1s;
  }

  .left-top-line2 {
    top: 110px;
    left: 10px;
    width: 4px;
    height: 50px;
  }

  .left-top-line,
  .left-top-line3,
  .left-top-line4,
  .left-top-line5,
  .left-bottom-line6 {
    display: none;
  }

  .content {
    padding: 0 16px 16px;
    font-size: 14px;
  }

  .arrow-container {
    bottom: 80px;
  }

  .card-container {
    margin-top: 24px;
  }

  .card {
    width: 86%;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 12px 10px 10px;
  }

  #home-card-avatar {
    width: 110px;
    height: 110px;
    margin: 2px 0 6px;
  }

  .card-content {
    width: 100%;
    padding: 0 4px 4px;
  }

  .card-title {
    font-size: 18px;
  }

  .card-line {
    font-size: 14px;
  }

  .social-icons {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-start;
    gap: 8px;
    margin-top: 10px;
  }

  .icon {
    width: 32px;
    height: 32px;
  }

  .posts-container {
    width: 92%;
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .post-cover {
    height: 130px;
  }

  .post-title {
    font-size: 16px;
  }

  .post-description {
    font-size: 12px;
    min-height: auto;
  }

  .post-date {
    font-size: 11px;
  }

  .empty-state {
    width: 92%;
  }

  .pagination {
    margin-top: 20px;
  }
}
</style>
