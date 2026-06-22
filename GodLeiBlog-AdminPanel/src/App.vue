<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Aside from './components/aside.vue'
import { logoutApi } from '@/api'
import { openSitePreviewInNewTab } from '@/utils/sitePreview'

const route = useRoute()
const router = useRouter()
const showAside = computed(() => route.path !== '/login')
const isMobile = ref(false)
const mobileMenuVisible = ref(false)
const ROUTE_TITLES = {
  Login: '后台登录',
  Home: '控制台',
  Article: '文章管理',
  Category: '分类管理',
  Tag: '标签管理',
  Moments: '朋友圈管理',
  Links: '友链管理',
  Logs: '日志管理',
  Settings: '站点设置',
  SiteSettings: '站点配置',
  HomeSettings: '首页管理',
  AboutSettings: '关于管理',
  AssistantSettings: 'AI 助手',
  LogsView: '日志预览',
  AddArticle: '发布文章',
  EditArticle: '编辑文章',
  Contact: '联系信息'
}

const pageTitle = computed(() => route.meta?.title || '管理台')
const pageDescription = computed(() => route.meta?.description || '')

const updateViewportState = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    mobileMenuVisible.value = false
  }
}

const openSitePreview = () => {
  openSitePreviewInNewTab('/', import.meta.env.VITE_SITE_PREVIEW_URL)
}

const toggleMobileMenu = () => {
  mobileMenuVisible.value = !mobileMenuVisible.value
}

const closeMobileMenu = () => {
  mobileMenuVisible.value = false
}

const syncDocumentTitle = (currentRoute) => {
  const title = ROUTE_TITLES[currentRoute.name] || '后台管理'
  document.title = `${title} | GodLeiBlog Admin`
}

const logout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    console.warn('退出登录接口执行失败，继续清理本地状态。', error)
  } finally {
    localStorage.removeItem('token')
    router.replace('/login')
  }
}

watch(
  () => route.path,
  () => {
    syncDocumentTitle(route)
    if (isMobile.value) {
      closeMobileMenu()
    }
  },
  { immediate: true }
)

onMounted(() => {
  updateViewportState()
  window.addEventListener('resize', updateViewportState)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportState)
})
</script>

<template>
  <div class="app-shell" :class="{ 'is-login': !showAside, 'is-mobile': isMobile }">
    <Aside
      v-if="showAside"
      :is-mobile="isMobile"
      :visible="!isMobile || mobileMenuVisible"
      @close="closeMobileMenu"
    />

    <div v-if="showAside && isMobile && mobileMenuVisible" class="app-mask" @click="closeMobileMenu"></div>

    <main class="app-main">
      <template v-if="showAside">
        <div class="app-main-inner">
          <header class="shell-topbar">
            <button v-if="isMobile" class="shell-icon-btn" type="button" @click="toggleMobileMenu">☰ 菜单</button>
            <div class="shell-spacer"></div>
            <button class="shell-text-btn" type="button" @click="openSitePreview">查看前台 ↗</button>
            <button class="shell-primary-btn" type="button" @click="logout">退出</button>
          </header>

          <div class="page-head">
            <h1>{{ pageTitle }}</h1>
            <span v-if="pageDescription" class="page-head__sub">{{ pageDescription }}</span>
          </div>

          <section class="page-body">
            <router-view />
          </section>
        </div>
      </template>

      <router-view v-else />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--admin-paper);
}

.app-main {
  flex: 1;
  min-width: 0;
}

.app-main-inner {
  width: min(1320px, calc(100% - 48px));
  margin: 0 auto;
  padding: 0 0 36px;
}

.shell-topbar {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--admin-border);
}

.shell-spacer { flex: 1; }

.shell-icon-btn,
.shell-text-btn,
.shell-primary-btn {
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  transition: all 0.15s ease;
}

.shell-icon-btn,
.shell-text-btn {
  border: 1px solid var(--admin-border-strong);
  background: var(--admin-panel);
  color: var(--admin-text);
}

.shell-icon-btn:hover,
.shell-text-btn:hover {
  border-color: var(--admin-accent);
  color: var(--admin-accent);
}

.shell-primary-btn {
  border: 1px solid var(--admin-crimson);
  background: var(--admin-crimson);
  color: #fff;
  font-weight: 500;
}

.shell-primary-btn:hover {
  background: #62141f;
  border-color: #62141f;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 20px;
}

.page-head h1 {
  position: relative;
  margin: 0;
  font-family: var(--admin-display);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.2px;
  padding-left: 14px;
}

.page-head h1::before {
  content: "";
  position: absolute;
  left: 0;
  top: 3px;
  bottom: 3px;
  width: 4px;
  border-radius: 2px;
  background: var(--admin-accent);
}

.page-head__sub {
  color: var(--admin-text-soft);
  font-size: 12.5px;
}

.page-body { min-width: 0; }

.app-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  z-index: 999;
}

.app-shell.is-login .app-main { padding: 0; }

@media (max-width: 980px) {
  .app-main-inner { width: min(100%, calc(100% - 24px)); }
}

@media (max-width: 768px) {
  .app-main-inner { width: min(100%, calc(100% - 20px)); }

  .shell-topbar {
    height: auto;
    padding: 12px 0;
    flex-wrap: wrap;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
