<script setup>
import { useRoute } from 'vue-router'
import { watch } from 'vue'
import Header from './components/header.vue'
import Footer from './components/Footer.vue'
import AssistantPanel from './components/AssistantPanel.vue'
import { setPageContext } from '@/utils/pageContext'

const route = useRoute()
const ROUTE_TITLES = {
  Home: '首页',
  About: '关于',
  Archive: '归档',
  Moments: '朋友圈',
  Links: '友链',
  Comments: '留言',
  Logs: '日志',
  PostDetail: '文章',
}

function syncBasePageContext(currentRoute) {
  const meta = currentRoute.meta || {}
  setPageContext({
    pageType: meta.pageType || 'page',
    route: currentRoute.fullPath || currentRoute.path || '/',
    title: meta.title || '',
    summary: meta.summary || '',
    contentExcerpt: '',
    momentId: null,
    currentMomentSummary: '',
  })
}

function syncDocumentTitle(currentRoute) {
  const title = ROUTE_TITLES[currentRoute.name] || ''
  document.title = title ? `${title} | GodLeiBlog` : 'GodLeiBlog'
}

watch(
  () => route.fullPath,
  () => {
    syncBasePageContext(route)
    syncDocumentTitle(route)
  },
  { immediate: true }
)
</script>

<template>
  <div class="app-root">
    <header>
      <Header />
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <Footer />
    <AssistantPanel />
  </div>
</template>

<style>
.app-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  padding-top: 68px;
}
</style>
