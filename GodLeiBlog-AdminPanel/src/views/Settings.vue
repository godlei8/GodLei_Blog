<template>
  <div class="settings-hub page-card">
    <el-tabs v-model="active" class="settings-hub__tabs">
      <el-tab-pane label="站点" name="basic" lazy>
        <SiteSettings />
      </el-tab-pane>
      <el-tab-pane label="首页" name="home" lazy>
        <HomeSettings />
      </el-tab-pane>
      <el-tab-pane label="关于" name="about" lazy>
        <AboutSettings />
      </el-tab-pane>
      <el-tab-pane label="AI 助手" name="assistant" lazy>
        <AssistantSettings />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { provide, ref } from 'vue'
import SiteSettings from './SiteSettings.vue'
import HomeSettings from './HomeSettings.vue'
import AboutSettings from './AboutSettings.vue'
import AssistantSettings from './AssistantSettings.vue'

// 让内部各 SiteConfigPageShell 进入嵌入模式（隐藏各自大标题，仅保留状态+保存）
provide('siteSettingsEmbedded', true)

const active = ref('basic')
</script>

<style scoped>
.settings-hub {
  padding: 16px;
}

/* 嵌入子页面去掉自身的外层卡片包裹，避免卡中卡 */
.settings-hub :deep(.site-settings.page-card) {
  border: none;
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.settings-hub__tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
}
.settings-hub__tabs :deep(.el-tabs__item.is-active) {
  color: var(--admin-text);
  font-weight: 600;
}
.settings-hub__tabs :deep(.el-tabs__active-bar) {
  background: var(--admin-accent);
}
</style>
