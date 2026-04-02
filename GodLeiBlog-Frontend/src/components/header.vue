<template>
  <header class="blog-header">
    <div class="header-left">
      <div class="glow-line"></div>
      <div class="header-title">
        <h1 ref="title" @click="goHome">GodLei Blog</h1>
      </div>
    </div>

    <div class="header-right">
      <nav class="header-nav">
        <ul>
          <li
            v-for="(item, index) in navItems"
            :key="index"
            class="nav-item"
          >
            <a href="#" @click.prevent="navigate(item.section)">{{ item.name }}</a>
            <div class="fluorescent-bar"></div>
          </li>
        </ul>
      </nav>

      <button
        class="hamburger-btn"
        :class="{ 'is-open': isMenuOpen }"
        aria-label="切换导航菜单"
        @click="toggleMenu"
      >
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>

    <transition name="side-drawer">
      <aside v-if="isMenuOpen" class="side-drawer" @click.self="closeMenu">
        <div class="side-drawer-panel">
          <div class="side-drawer-header">
            <div class="glow-line glow-line--small"></div>
            <span class="side-drawer-title">导航菜单</span>
          </div>
          <ul class="side-drawer-list">
            <li
              v-for="(item, index) in navItems"
              :key="`side-${index}`"
              class="side-drawer-item"
              @click="handleMenuClick(item.section)"
            >
              <span>{{ item.name }}</span>
              <div class="side-drawer-bar"></div>
            </li>
          </ul>
        </div>
      </aside>
    </transition>

    <div class="header-divider"></div>
  </header>
</template>

<script>
export default {
  name: 'HeaderBar',
  data() {
    return {
      navItems: [
        { name: '首页', section: '' },
        { name: '关于', section: 'about' },
        { name: '归档', section: 'archive' },
        { name: '朋友圈', section: 'moments' },
        { name: '友链', section: 'links' },
        { name: '留言', section: 'comments' },
        { name: '日志', section: 'logs' },
      ],
      isMenuOpen: false,
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.animateTitle()
      this.animateNavItems()
    })

    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    animateTitle() {
      const titleElement = this.$refs.title
      if (!titleElement) return

      const text = titleElement.textContent
      titleElement.innerHTML = ''

      text.split('').forEach((char, index) => {
        const span = document.createElement('span')
        span.textContent = char === ' ' ? '\u00A0' : char
        span.style.opacity = '0'
        span.style.transform = 'translateY(20px) scale(0.5)'
        span.style.display = 'inline-block'
        span.style.transition = `all 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275) ${index * 0.08}s`
        titleElement.appendChild(span)

        setTimeout(() => {
          span.style.opacity = '1'
          span.style.transform = 'translateY(0) scale(1)'
        }, index * 90)
      })
    },
    animateNavItems() {
      this.$nextTick(() => {
        const navItems = document.querySelectorAll('.nav-item')

        navItems.forEach((item, index) => {
          item.style.opacity = '0'
          item.style.transform = 'translateX(100%)'
          item.style.position = 'relative'

          setTimeout(() => {
            item.style.transition = 'all 0.6s ease-out'
            item.style.transform = 'translateX(0)'
            item.style.opacity = '1'
          }, index * 160 + 320)
        })
      })
    },
    goHome() {
      this.$router.push('/')
      this.closeMenu()
    },
    navigate(section) {
      this.$router.push(`/${section}`)
      this.closeMenu()
    },
    toggleMenu() {
      this.isMenuOpen = !this.isMenuOpen
    },
    closeMenu() {
      this.isMenuOpen = false
    },
    handleMenuClick(section) {
      this.navigate(section)
      this.closeMenu()
    },
    handleResize() {
      if (window.innerWidth > 768 && this.isMenuOpen) {
        this.isMenuOpen = false
      }
    },
  },
}
</script>

<style>
.blog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 40px;
  background: rgba(9, 3, 5, 0.82);
  color: rgba(255, 247, 234, 0.92);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
  box-sizing: border-box;
  backdrop-filter: blur(14px);
}

.header-left {
  display: flex;
  flex-direction: row;
  gap: 2vw;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.glow-line {
  width: 4px;
  height: 40px;
  background: var(--theme-accent-line-vertical);
  border-radius: 2px;
  box-shadow: 4px 0 10px rgba(214, 173, 92, 0.46), 14px 0 18px rgba(122, 29, 45, 0.34);
  animation: glow-pulse 2s infinite alternate;
}

.glow-line--small {
  width: 3px;
  height: 24px;
}

@keyframes glow-pulse {
  0% {
    opacity: 0.64;
    transform: scaleY(1);
  }

  100% {
    opacity: 1;
    transform: scaleY(1.18);
  }
}

.header-title h1 {
  margin: 0;
  font-size: 24px;
  cursor: pointer;
  transition: color 0.3s ease;
}

.header-title h1:hover {
  color: var(--theme-accent-text);
}

.header-title h1 span {
  display: inline-block;
  font-size: 24px;
  font-weight: bold;
  color: var(--theme-accent-text-strong);
  transition: all 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.header-title h1:hover span {
  color: var(--theme-accent-text);
  text-shadow: 0 0 5px rgba(214, 173, 92, 0.42);
}

.header-nav {
  overflow: visible;
  white-space: nowrap;
}

.header-nav ul {
  list-style: none;
  display: flex;
  gap: 30px;
  width: 100%;
  min-width: max-content;
}

.header-nav a {
  text-decoration: none;
  color: rgba(255, 239, 211, 0.84);
  font-size: 18px;
  transition: color 0.3s ease;
}

.header-nav a:hover {
  color: var(--theme-accent-text);
}

.header-divider {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 1px;
  background: var(--theme-accent-line-horizontal);
  z-index: 1001;
}

.nav-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  opacity: 0;
  transform: translateX(100%);
  transition: all 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  overflow: visible;
}

.fluorescent-bar {
  position: absolute;
  bottom: -19px;
  transform: translateY(100%);
  width: 130%;
  height: 1px;
  background: linear-gradient(90deg, rgba(122, 29, 45, 0), rgba(214, 173, 92, 1), rgba(122, 29, 45, 0));
  box-shadow: 0 -10px 20px rgba(214, 173, 92, 0.42);
  opacity: 1;
  z-index: 1002;
}

.hamburger-btn {
  position: relative;
  width: 32px;
  height: 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  display: none;
}

.hamburger-btn span {
  position: absolute;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, var(--theme-accent-text), #f5ddb1, var(--theme-accent-text));
  border-radius: 999px;
  box-shadow: 0 0 8px rgba(214, 173, 92, 0.42);
  transition: transform 0.25s ease, opacity 0.2s ease, top 0.25s ease;
}

.hamburger-btn span:nth-child(1) {
  top: 0;
}

.hamburger-btn span:nth-child(2) {
  top: 10px;
}

.hamburger-btn span:nth-child(3) {
  top: 20px;
}

.hamburger-btn.is-open span:nth-child(1) {
  top: 10px;
  transform: rotate(45deg);
}

.hamburger-btn.is-open span:nth-child(2) {
  opacity: 0;
}

.hamburger-btn.is-open span:nth-child(3) {
  top: 10px;
  transform: rotate(-45deg);
}

.side-drawer {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.62);
  backdrop-filter: blur(4px);
  z-index: 1200;
  display: flex;
  justify-content: flex-end;
}

.side-drawer-panel {
  width: 70%;
  max-width: 320px;
  height: 100%;
  background: radial-gradient(circle at top left, rgba(122, 29, 45, 0.34), rgba(5, 2, 3, 0.96));
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.8);
  border-left: 1px solid rgba(214, 173, 92, 0.2);
  display: flex;
  flex-direction: column;
  padding: 18px 18px 28px;
}

.side-drawer-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.side-drawer-title {
  font-size: 18px;
  color: var(--theme-accent-text-strong);
  letter-spacing: 0.08em;
}

.side-drawer-list {
  list-style: none;
  margin: 0;
  padding: 8px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.side-drawer-item {
  position: relative;
  padding: 10px 4px;
  color: var(--theme-accent-text-strong);
  font-size: 16px;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: color 0.2s ease;
}

.side-drawer-item span {
  z-index: 1;
}

.side-drawer-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(122, 29, 45, 0), rgba(214, 173, 92, 1), rgba(122, 29, 45, 0));
  box-shadow: 0 -10px 24px rgba(214, 173, 92, 0.32);
  transition: width 0.3s ease;
}

.side-drawer-item:hover {
  color: var(--theme-accent-text);
}

.side-drawer-item:hover .side-drawer-bar {
  width: 100%;
}

.side-drawer-enter-active,
.side-drawer-leave-active {
  transition: opacity 0.25s ease;
}

.side-drawer-enter-from,
.side-drawer-leave-to {
  opacity: 0;
}

.side-drawer-panel {
  transform: translateX(0);
  transition: transform 0.25s ease;
}

.side-drawer-enter-from .side-drawer-panel,
.side-drawer-leave-to .side-drawer-panel {
  transform: translateX(100%);
}

@media (max-width: 768px) {
  .blog-header {
    padding: 10px 25px;
  }

  .header-left {
    gap: 10px;
  }

  .header-nav {
    display: none;
  }

  .hamburger-btn {
    display: block;
  }
}
</style>
