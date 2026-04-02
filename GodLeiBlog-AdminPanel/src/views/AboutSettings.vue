<template>
  <SiteConfigPageShell
    eyebrow="About"
    title="关于管理"
    description="关于页内容独立维护，专门管理我的追番封面列表。"
    :status-text="statusText"
    :status-description="statusDescription"
    :is-dirty="isDirty"
    :loading="loading"
    :saving="saving"
    :summary-items="summaryItems"
  >
    <template #actions>
      <el-button @click="loadConfig" :loading="loading">重新加载</el-button>
      <el-button @click="openPreview('/about')">预览关于页</el-button>
      <el-button type="primary" :loading="saving" @click="saveSection">保存关于页</el-button>
    </template>

    <section class="settings-section">
      <header class="settings-section__head">
        <div>
          <h3>我的追番</h3>
          <p>维护关于页展示的追番封面列表，顺序会直接影响前台展示。</p>
        </div>
        <el-button @click="appendUploadedItem('about.animeImages', 'anime-poster')">上传新封面</el-button>
      </header>

      <div class="settings-array__list settings-array__list--media settings-array__list--poster-wall">
        <div
          v-for="(image, index) in form.about.animeImages"
          :key="`anime-${index}`"
          class="settings-media-item settings-media-item--poster settings-media-item--poster-tall"
        >
          <img :src="previewUrl(image)" :alt="`anime-${index}`" />
          <div class="settings-media-item__body">
            <el-input v-model="form.about.animeImages[index]" placeholder="输入封面地址" clearable />
            <div class="settings-media-item__actions">
              <el-button @click="replaceUploadedItem('about.animeImages', index, 'anime-poster')">替换封面</el-button>
              <el-button type="danger" plain @click="removeTextItem(form.about.animeImages, index)">删除</el-button>
            </div>
          </div>
        </div>
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
  removeTextItem,
  appendUploadedItem,
  replaceUploadedItem
} = useSiteConfigSection('about')

const summaryItems = computed(() => ([
  { label: '追番数量', value: `${form.about.animeImages.length} 张` },
  { label: '封面状态', value: form.about.animeImages[0] ? '已配置' : '未配置' },
  { label: '当前状态', value: statusText.value }
]))

onMounted(() => {
  loadConfig()
})
</script>
