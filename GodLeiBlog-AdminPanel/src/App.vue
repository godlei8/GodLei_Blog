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
  SiteSettings: '站点配置',
  HomeSettings: '首页管理',
  AboutSettings: '关于管理',
  AssistantSettings: 'AI 助手',
  LogsView: '日志预览',
  AddArticle: '发布文章',
  EditArticle: '编辑文章',
  Contact: '联系信息'
}

const pageEyebrow = computed(() => route.meta?.eyebrow || 'Admin')
const pageTitle = computed(() => route.meta?.title || '管理台')
const pageDescription = computed(() => route.meta?.description || '保持站点内容、配置与运营数据的统一更新。')

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
            <div class="shell-topbar__left">
              <button v-if="isMobile" class="shell-icon-btn" type="button" @click="toggleMobileMenu">菜单</button>
              <div class="shell-brand">
                <span class="shell-brand__eyebrow">GodLei Blog Admin</span>
                <strong>内容管理</strong>
              </div>
            </div>

            <div class="shell-topbar__actions">
              <button class="shell-text-btn" type="button" @click="openSitePreview">打开前台</button>
              <button class="shell-primary-btn" type="button" @click="logout">退出登录</button>
            </div>
          </header>

          <section class="page-hero">
            <div class="page-hero__copy">
              <span class="page-hero__eyebrow">{{ pageEyebrow }}</span>
              <h1>{{ pageTitle }}</h1>
              <p>{{ pageDescription }}</p>
            </div>

            <div class="page-hero__meta">
              <div class="page-hero__chip">
                <span>当前路由</span>
                <strong>{{ route.path }}</strong>
              </div>
            </div>
          </section>

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
  background:
    radial-gradient(circle at top left, rgba(212, 182, 116, 0.14), transparent 24%),
    linear-gradient(180deg, #fcfaf5 0%, #f7f1e7 100%);
}

.app-main {
  flex: 1;
  min-width: 0;
}

.app-main-inner {
  width: min(1480px, calc(100% - 32px));
  margin: 0 auto;
  padding: 14px 0 24px;
}

.shell-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 0 14px;
}

.shell-topbar__left,
.shell-topbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shell-brand {
  display: grid;
  gap: 2px;
}

.shell-brand__eyebrow {
  color: var(--admin-text-soft);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.shell-brand strong {
  color: var(--admin-text);
  font-size: 18px;
}

.shell-icon-btn,
.shell-text-btn,
.shell-primary-btn {
  border-radius: 999px;
  padding: 8px 12px;
  cursor: pointer;
  font: inherit;
}

.shell-icon-btn,
.shell-text-btn {
  border: 1px solid var(--admin-border);
  background: rgba(255, 255, 255, 0.84);
  color: var(--admin-text);
}

.shell-primary-btn {
  border: none;
  background: linear-gradient(135deg, var(--admin-accent), var(--admin-accent-strong));
  color: #2d1b07;
  font-weight: 600;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 300px);
  gap: 14px;
  margin-bottom: 14px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid var(--admin-border-soft);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(250, 246, 238, 0.96)),
    rgba(255, 255, 255, 0.98);
  box-shadow: 0 12px 24px rgba(112, 84, 34, 0.06);
}

.page-hero__eyebrow {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(214, 173, 92, 0.14);
  color: #8a6427;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-hero h1 {
  margin: 12px 0 8px;
  font-size: clamp(26px, 3.6vw, 34px);
  line-height: 1.05;
}

.page-hero p {
  margin: 0;
  color: var(--admin-text-muted);
  line-height: 1.7;
}

.page-hero__meta {
  display: grid;
  gap: 10px;
  align-content: start;
}

.page-hero__chip {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(214, 173, 92, 0.12);
}

.page-hero__chip span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
  margin-bottom: 6px;
}

.page-hero__chip strong {
  color: var(--admin-text);
  font-size: 15px;
  word-break: break-all;
}

.page-body {
  min-width: 0;
}

.app-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 999;
}

.app-shell.is-login .app-main {
  padding: 0;
}

@media (max-width: 980px) {
  .app-main-inner {
    width: min(100%, calc(100% - 24px));
  }

  .page-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .app-main-inner {
    width: min(100%, calc(100% - 20px));
    padding-top: 10px;
  }

  .shell-topbar,
  .shell-topbar__left,
  .shell-topbar__actions {
    flex-wrap: wrap;
  }

  .shell-topbar__actions {
    width: 100%;
  }

  .shell-text-btn,
  .shell-primary-btn {
    flex: 1;
    justify-content: center;
  }

  .page-hero {
    padding: 16px;
    border-radius: 18px;
  }
}
</style>
