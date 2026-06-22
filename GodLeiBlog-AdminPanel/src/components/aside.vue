<template>
  <aside class="sidebar" :class="{ 'is-mobile': isMobile, 'is-visible': visible }">
    <div class="brand">
      <span class="brand__mark">G</span>
      <span class="brand__name">
        <strong>GodLei Blog</strong>
        <small>管理台</small>
      </span>
    </div>

    <nav class="nav">
      <template v-for="(group, gi) in groups" :key="gi">
        <div v-if="group.label" class="nav__group">{{ group.label }}</div>
        <a
          v-for="item in group.items"
          :key="item.path"
          class="nav__item"
          :class="{ active: isActive(item.path) }"
          @click="go(item.path)"
        >
          <span class="nav__icon" v-html="item.icon"></span>
          <span>{{ item.label }}</span>
        </a>
      </template>
    </nav>
  </aside>
</template>

<script>
const I = {
  dash: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></svg>',
  article: '<svg viewBox="0 0 24 24"><path d="M5 3h14v18H5zM9 8h6M9 12h6M9 16h4"/></svg>',
  category: '<svg viewBox="0 0 24 24"><path d="M3 7l2-3h5l1.6 2.4H21V20H3z"/></svg>',
  tag: '<svg viewBox="0 0 24 24"><path d="M3 7v5l9 9 6-6-9-9H3z"/><circle cx="7.5" cy="11" r="1.3"/></svg>',
  moment: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"/></svg>',
  link: '<svg viewBox="0 0 24 24"><path d="M9 15l6-6M10 6l1-1a4 4 0 0 1 6 6l-1 1M14 18l-1 1a4 4 0 0 1-6-6l1-1"/></svg>',
  log: '<svg viewBox="0 0 24 24"><path d="M4 5h16M4 12h16M4 19h10"/></svg>',
  site: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M4 12h2M18 12h2M12 4v2M12 18v2M6 6l1.5 1.5M16.5 16.5L18 18M18 6l-1.5 1.5M7.5 16.5L6 18"/></svg>',
  home: '<svg viewBox="0 0 24 24"><path d="M4 11l8-7 8 7M6 10v9h12v-9"/></svg>',
  about: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.6h.01"/></svg>',
  ai: '<svg viewBox="0 0 24 24"><rect x="4" y="7" width="16" height="12" rx="3"/><path d="M9 12h.01M15 12h.01M12 3v4"/></svg>'
}

export default {
  props: {
    isMobile: { type: Boolean, default: false },
    visible: { type: Boolean, default: true }
  },
  data() {
    return {
      groups: [
        { items: [{ path: '/', label: '控制台', icon: I.dash }] },
        {
          label: '内容',
          items: [
            { path: '/article', label: '文章', icon: I.article },
            { path: '/category', label: '分类', icon: I.category },
            { path: '/tag', label: '标签', icon: I.tag },
            { path: '/moments', label: '朋友圈', icon: I.moment }
          ]
        },
        {
          label: '运营',
          items: [
            { path: '/links', label: '友链', icon: I.link },
            { path: '/logs', label: '日志', icon: I.log }
          ]
        },
        {
          label: '设置',
          items: [
            { path: '/site-settings', label: '站点配置', icon: I.site },
            { path: '/home-settings', label: '首页', icon: I.home },
            { path: '/about-settings', label: '关于', icon: I.about },
            { path: '/assistant-settings', label: 'AI 助手', icon: I.ai }
          ]
        }
      ]
    }
  },
  methods: {
    isActive(path) {
      const cur = this.$route.path
      if (path === '/') return cur === '/'
      if (path === '/article') return cur.startsWith('/article') || cur.startsWith('/add-article') || cur.startsWith('/edit-article')
      return cur === path || cur.startsWith(path + '/')
    },
    go(path) {
      if (this.$route.path !== path) this.$router.push(path)
      if (this.isMobile) this.$emit('close')
    }
  }
}
</script>

<style>
.sidebar {
  width: 224px;
  height: 100vh;
  background: var(--admin-panel);
  border-right: 1px solid var(--admin-border);
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--admin-border);
}

.brand__mark {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  color: #fff;
  font-family: var(--admin-display);
  font-weight: 700;
  font-size: 14px;
  background: linear-gradient(135deg, var(--admin-accent), var(--admin-accent-strong));
}

.brand__name strong {
  display: block;
  font-family: var(--admin-display);
  font-size: 15px;
  line-height: 1.15;
  color: var(--admin-text);
}

.brand__name small {
  font-family: var(--admin-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--admin-text-soft);
}

.nav {
  padding: 12px;
  overflow-y: auto;
  flex: 1;
}

.nav__group {
  margin: 18px 0 6px;
  padding: 0 10px;
  font-family: var(--admin-mono);
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--admin-text-soft);
}

.nav__item {
  display: flex;
  align-items: center;
  gap: 11px;
  position: relative;
  margin: 1px 0;
  padding: 9px 12px;
  border-radius: 8px;
  color: var(--admin-text-muted);
  cursor: pointer;
  font-size: 13.5px;
  transition: background 0.14s ease, color 0.14s ease;
}

.nav__item:hover {
  background: var(--admin-panel-soft);
  color: var(--admin-text);
}

.nav__item.active {
  color: var(--admin-text);
  font-weight: 600;
  background: var(--admin-accent-tint);
}

.nav__item.active::before {
  content: "";
  position: absolute;
  left: -12px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--admin-accent);
}

.nav__icon {
  width: 17px;
  height: 17px;
  display: inline-flex;
  flex: 0 0 auto;
}

.nav__icon svg {
  width: 17px;
  height: 17px;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

@media (max-width: 768px) {
  .sidebar.is-mobile {
    position: fixed;
    left: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
  }

  .sidebar.is-mobile.is-visible {
    transform: translateX(0);
  }
}
</style>
