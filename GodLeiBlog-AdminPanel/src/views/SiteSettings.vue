<template>
  <SiteConfigPageShell
    eyebrow="Site"
    title="站点配置"
    description="集中维护全站通用基础信息，站点名称和头像会在首页、文章页与公共导航区域复用。"
    :status-text="statusText"
    :status-description="statusDescription"
    :is-dirty="isDirty"
    :loading="loading"
    :saving="saving"
    :summary-items="summaryItems"
  >
    <template #actions>
      <el-button @click="loadConfig" :loading="loading">重新加载</el-button>
      <el-button @click="openPreview('/')">预览首页</el-button>
      <el-button type="primary" :loading="saving" @click="saveSection">保存配置</el-button>
    </template>

    <section class="settings-section">
      <header class="settings-section__head">
        <div>
          <h3>基础信息</h3>
          <p>站点名称和头像会在首页、关于页、文章页等位置复用。</p>
        </div>
      </header>

      <div class="settings-basic-grid">
        <div class="settings-basic-main">
          <label class="settings-field">
            <span>站点名称</span>
            <el-input v-model="form.basic.siteName" placeholder="GodLei Blog" />
            <div class="settings-name-preview">
              <strong>{{ form.basic.siteName || 'GodLei Blog' }}</strong>
              <span>当前站点名称预览</span>
            </div>
            <small class="settings-field__hint">这里的名称会同步出现在首页标题、头像信息区和公共导航区域。</small>
          </label>

          <label class="settings-field">
            <span>头像地址</span>
            <div class="settings-inline">
              <el-input
                v-model="form.basic.profileAvatar"
                placeholder="输入头像地址或上传图片"
                clearable
              />
              <el-button @click="uploadField('basic.profileAvatar', 'site-avatar')">上传头像</el-button>
            </div>
            <small class="settings-field__hint">建议使用清晰的正方形图片，上传后会自动同步到前台展示。</small>
          </label>
        </div>

        <aside class="settings-avatar-preview settings-avatar-preview--profile">
          <div class="settings-avatar-preview__head">
            <strong>当前头像预览</strong>
            <span>{{ form.basic.profileAvatar ? '已配置自定义头像' : '当前为默认头像' }}</span>
          </div>
          <img
            class="settings-preview__avatar"
            :src="previewUrl(form.basic.profileAvatar)"
            alt="avatar preview"
          />
        </aside>
      </div>
    </section>
  </SiteConfigPageShell>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import SiteConfigPageShell from '@/components/SiteConfigPageShell.vue'
import { useSiteConfigSection } from '@/composables/useSiteConfigSection'
import '@/assets/site-config-section.css'

const {
  form,
  loading,
  saving,
  isDirty,
  statusText,
  statusDescription,
  previewUrl,
  loadConfig,
  saveSection,
  openPreview,
  uploadField,
} = useSiteConfigSection('basic')

const summaryItems = computed(() => ([
  { label: '站点名称', value: form.basic.siteName || '未设置' },
  { label: '头像状态', value: form.basic.profileAvatar ? '已配置' : '未配置' },
  { label: '当前状态', value: statusText.value },
]))

onMounted(() => {
  loadConfig()
})
</script>
