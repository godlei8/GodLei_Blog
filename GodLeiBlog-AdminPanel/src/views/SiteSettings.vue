<template>
  <div class="site-settings page-card">
    <section class="settings-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">Site Settings</span>
        <h2>站点配置</h2>
        <p>
          集中管理首页背景、个人头像和关于页追番封面。上传后的图片会自动保存，前台切回页面时会重新读取最新配置。
        </p>

        <div class="hero-meta">
          <span class="status-pill" :class="{ 'is-dirty': isDirty, 'is-busy': loading || saving }">
            {{ statusText }}
          </span>
          <span class="hero-meta-text">{{ statusDescription }}</span>
        </div>
      </div>

      <div class="hero-actions">
        <el-button @click="loadConfig" :loading="loading">重新加载</el-button>
        <el-button plain @click="openPreview('/')">预览首页</el-button>
        <el-button plain @click="openPreview('/about')">预览关于页</el-button>
        <el-button type="primary" @click="saveSettings" :loading="saving">保存配置</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">个人头像</span>
        <strong>{{ form.basic.profileAvatar ? '已设置' : '使用默认图' }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">首页背景</span>
        <strong>{{ form.home.backgroundImage ? '已设置' : '使用默认图' }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">追番封面</span>
        <strong>{{ animeCountText }}</strong>
      </div>
    </section>

    <div class="settings-grid">
      <section class="settings-panel">
        <div class="panel-head">
          <div>
            <h3>个人头像</h3>
            <p>会同时用于首页卡片和关于页头图，建议使用清晰的方形图片。</p>
          </div>
        </div>

        <div class="field-layout">
          <div class="field-main">
            <label class="field-label" for="avatar-url">头像地址</label>
            <div class="image-input-row">
              <el-input
                id="avatar-url"
                v-model="form.basic.profileAvatar"
                placeholder="请输入头像图片地址"
                clearable
              />
              <el-button @click="uploadFieldImage('basic.profileAvatar', 'site-avatar')">上传头像</el-button>
            </div>
            <p class="field-hint">支持手动填写 URL，也支持上传后自动生成 `/api/uploads/...` 路径。</p>
          </div>

          <div class="preview-card">
            <span class="preview-label">头像预览</span>
            <div v-if="form.basic.profileAvatar" class="image-preview">
              <img :src="previewUrl(form.basic.profileAvatar)" alt="profile avatar preview" />
            </div>
            <div v-else class="preview-placeholder">
              未设置时，前台会自动回退到默认头像。
            </div>
          </div>
        </div>
      </section>

      <section class="settings-panel">
        <div class="panel-head">
          <div>
            <h3>首页背景图</h3>
            <p>用于首页欢迎区的大背景图，建议选择横向图片，避免主体过于靠边。</p>
          </div>
        </div>

        <div class="field-layout field-layout--stack">
          <div class="field-main">
            <label class="field-label" for="background-url">背景地址</label>
            <div class="image-input-row">
              <el-input
                id="background-url"
                v-model="form.home.backgroundImage"
                placeholder="请输入首页背景图地址"
                clearable
              />
              <el-button @click="uploadFieldImage('home.backgroundImage', 'home-background')">上传背景图</el-button>
            </div>
            <p class="field-hint">保存后首页欢迎区会优先使用这里的图片；留空时回退到默认背景。</p>
          </div>

          <div class="preview-card preview-card--wide">
            <span class="preview-label">背景预览</span>
            <div v-if="form.home.backgroundImage" class="image-preview image-preview--wide">
              <img :src="previewUrl(form.home.backgroundImage)" alt="home background preview" />
            </div>
            <div v-else class="preview-placeholder preview-placeholder--wide">
              还没有设置首页背景图。
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="settings-panel settings-panel--full">
      <div class="panel-head panel-head--space">
        <div>
          <h3>关于页追番封面</h3>
          <p>支持手动填写图片地址或直接上传，列表会按当前顺序展示在关于页。</p>
        </div>

        <div class="panel-actions">
          <el-button @click="addAnimeImage">新增一项</el-button>
          <el-button @click="appendUploadedAnimeImage">上传图片</el-button>
        </div>
      </div>

      <div v-if="!form.about.animeImages.length" class="empty-state">
        还没有配置追番封面，点击“新增一项”或“上传图片”即可开始添加。
      </div>

      <div v-else class="anime-list">
        <div
          v-for="(image, index) in form.about.animeImages"
          :key="`anime-${index}`"
          class="anime-item"
        >
          <div class="anime-preview-shell">
            <div v-if="image" class="image-preview anime-preview">
              <img :src="previewUrl(image)" :alt="`anime preview ${index + 1}`" />
            </div>
            <div v-else class="preview-placeholder anime-preview anime-preview--empty">
              等待填写或上传封面
            </div>
          </div>

          <div class="anime-item-body">
            <div class="anime-item-head">
              <div>
                <span class="anime-title">封面 {{ index + 1 }}</span>
                <p class="anime-description">推荐使用竖图，方便关于页网格展示。</p>
              </div>

              <div class="anime-item-actions">
                <el-button @click="uploadAnimeImage(index)">替换图片</el-button>
                <el-button type="danger" plain @click="removeAnimeImage(index)">删除</el-button>
              </div>
            </div>

            <el-input
              v-model="form.about.animeImages[index]"
              placeholder="请输入追番封面图片地址"
              clearable
            />

            <p class="field-hint field-hint--compact">
              当前项支持手动输入 URL；如果是后台上传的图片，替换后会自动保存。
            </p>
          </div>
        </div>
      </div>
    </section>

    <transition name="save-bar">
      <div v-if="isDirty" class="save-bar">
        <div class="save-bar-copy">
          <strong>你有未保存的修改</strong>
          <p>手动填写链接、清空图片或调整追番封面后，记得保存一次。</p>
        </div>

        <div class="save-bar-actions">
          <el-button @click="loadConfig" :loading="loading">放弃修改</el-button>
          <el-button type="primary" @click="saveSettings" :loading="saving">保存配置</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { getSiteConfig, saveSiteConfig, uploadImage } from '@/api'

const SITE_CONFIG_UPDATE_KEY = 'godlei-site-config-updated-at'
const PREVIEW_BASE_URL = (import.meta.env.VITE_SITE_PREVIEW_URL || '').trim()

const normalizeMediaUrl = (value) => {
  const source = typeof value === 'string' ? value.trim() : ''
  if (!source) return ''
  if (source.startsWith('/api/uploads/')) return source
  if (source.startsWith('/uploads/')) return `/api${source}`
  if (source.startsWith('uploads/')) return `/api/${source}`
  return source
}

const normalizeStringList = (list = []) => {
  if (!Array.isArray(list)) return []
  return list
    .map((item) => normalizeMediaUrl(item))
    .filter(Boolean)
}

const createDefaultConfig = () => ({
  basic: {
    siteName: 'GodLei Blog',
    profileAvatar: ''
  },
  home: {
    backgroundImage: ''
  },
  about: {
    animeImages: []
  }
})

export default {
  name: 'SiteSettings',
  data() {
    return {
      loading: false,
      saving: false,
      form: createDefaultConfig(),
      savedSnapshot: JSON.stringify(createDefaultConfig()),
      lastSyncedAt: ''
    }
  },
  computed: {
    isDirty() {
      return JSON.stringify(this.buildPayload()) !== this.savedSnapshot
    },
    statusText() {
      if (this.saving) return '保存中'
      if (this.loading) return '加载中'
      if (this.isDirty) return '待保存'
      return '已同步'
    },
    statusDescription() {
      if (this.saving) return '正在写入站点配置，请稍候。'
      if (this.loading) return '正在读取当前配置。'
      if (this.isDirty) return '当前表单有改动，保存后前台会重新读取最新图片。'
      if (this.lastSyncedAt) return `最近同步于 ${this.lastSyncedAt}`
      return '当前配置已经和服务器保持一致。'
    },
    animeCountText() {
      const count = this.form.about.animeImages.length
      return count ? `${count} 张` : '未设置'
    },
    previewBaseUrl() {
      if (PREVIEW_BASE_URL) return PREVIEW_BASE_URL.replace(/\/$/, '')
      if (typeof window === 'undefined') return ''
      const { protocol, hostname, origin, port } = window.location
      if (port === '5174') {
        return `${protocol}//${hostname}:5173`
      }
      return origin
    }
  },
  created() {
    this.loadConfig()
  },
  methods: {
    clone(value) {
      return JSON.parse(JSON.stringify(value))
    },
    formatNow() {
      return new Date().toLocaleString('zh-CN', { hour12: false })
    },
    syncSnapshot() {
      this.savedSnapshot = JSON.stringify(this.buildPayload())
      this.lastSyncedAt = this.formatNow()
    },
    notifyFrontendConfigUpdated() {
      if (typeof window === 'undefined' || !window.localStorage) return
      window.localStorage.setItem(SITE_CONFIG_UPDATE_KEY, String(Date.now()))
    },
    mergeConfig(source = {}) {
      const defaults = createDefaultConfig()
      const incoming = source || {}

      return {
        basic: {
          ...defaults.basic,
          ...(incoming.basic || {}),
          profileAvatar: normalizeMediaUrl(incoming.basic?.profileAvatar || defaults.basic.profileAvatar)
        },
        home: {
          ...defaults.home,
          ...(incoming.home || {}),
          backgroundImage: normalizeMediaUrl(incoming.home?.backgroundImage || defaults.home.backgroundImage)
        },
        about: {
          animeImages: normalizeStringList(incoming.about?.animeImages || defaults.about.animeImages)
        }
      }
    },
    previewUrl(url) {
      return normalizeMediaUrl(url)
    },
    buildPayload() {
      const payload = this.clone(this.form)
      payload.basic.profileAvatar = normalizeMediaUrl(payload.basic.profileAvatar)
      payload.home.backgroundImage = normalizeMediaUrl(payload.home.backgroundImage)
      payload.about.animeImages = normalizeStringList(payload.about.animeImages)
      return payload
    },
    async loadConfig() {
      this.loading = true
      try {
        const data = await getSiteConfig()
        this.form = this.mergeConfig(data)
        this.syncSnapshot()
      } catch (error) {
        console.error('加载站点配置失败:', error)
        this.$message.error('加载站点配置失败，请稍后重试')
        this.form = createDefaultConfig()
        this.syncSnapshot()
      } finally {
        this.loading = false
      }
    },
    async saveSettings(options = {}) {
      const { successMessage = '站点配置已保存' } = options
      this.saving = true
      try {
        await saveSiteConfig(this.buildPayload())
        this.syncSnapshot()
        this.notifyFrontendConfigUpdated()
        this.$message.success(successMessage)
      } catch (error) {
        console.error('保存站点配置失败:', error)
        this.$message.error('保存失败，请稍后重试')
      } finally {
        this.saving = false
      }
    },
    async pickAndUpload(bizType) {
      return new Promise((resolve) => {
        const input = document.createElement('input')
        input.type = 'file'
        input.accept = 'image/*'
        input.onchange = async () => {
          const files = Array.from(input.files || [])
          if (!files.length) {
            resolve(null)
            return
          }

          try {
            const uploaded = await uploadImage(files[0], bizType)
            resolve(uploaded)
          } catch (error) {
            console.error('上传图片失败:', error)
            this.$message.error('图片上传失败，请稍后重试')
            resolve(null)
          }
        }
        input.click()
      })
    },
    async persistImageChange(successMessage) {
      await this.saveSettings({ successMessage })
    },
    async uploadFieldImage(path, bizType) {
      const result = await this.pickAndUpload(bizType)
      if (!result?.url) return
      this.setValueByPath(path, normalizeMediaUrl(result.url))
      await this.persistImageChange('图片已上传并自动保存')
    },
    async appendUploadedAnimeImage() {
      const result = await this.pickAndUpload('anime-poster')
      if (!result?.url) return
      this.form.about.animeImages.push(normalizeMediaUrl(result.url))
      await this.persistImageChange('追番封面已上传并自动保存')
    },
    async uploadAnimeImage(index) {
      const result = await this.pickAndUpload('anime-poster')
      if (!result?.url) return
      this.form.about.animeImages.splice(index, 1, normalizeMediaUrl(result.url))
      await this.persistImageChange('追番封面已替换并自动保存')
    },
    addAnimeImage() {
      this.form.about.animeImages.push('')
    },
    removeAnimeImage(index) {
      this.form.about.animeImages.splice(index, 1)
    },
    setValueByPath(path, value) {
      const keys = path.split('.')
      let current = this.form
      keys.forEach((key, index) => {
        if (index === keys.length - 1) {
          current[key] = value
          return
        }
        current = current[key]
      })
    },
    openPreview(path = '/') {
      const target = this.previewBaseUrl || (typeof window !== 'undefined' ? window.location.origin : '')
      if (!target) return
      const url = new URL(path, `${target}/`).toString()
      window.open(url, '_blank', 'noopener')
    }
  }
}
</script>

<style scoped>
.site-settings {
  padding: 22px;
}

.settings-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 4px 2px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.hero-copy {
  max-width: 720px;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.15;
}

.hero-copy p {
  margin: 0;
  color: var(--admin-text-muted);
  line-height: 1.8;
}

.hero-meta {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--admin-text);
  font-size: 13px;
  font-weight: 600;
}

.status-pill.is-dirty {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.status-pill.is-busy {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.hero-meta-text {
  color: var(--admin-text-muted);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 260px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.summary-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--admin-border-soft);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96)),
    rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.04);
}

.summary-label {
  display: block;
  color: var(--admin-text-muted);
  font-size: 13px;
  margin-bottom: 6px;
}

.summary-card strong {
  font-size: 22px;
  font-weight: 700;
  color: var(--admin-text);
}

.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
  margin-top: 18px;
}

.settings-panel {
  border: 1px solid var(--admin-border-soft);
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.04);
  padding: 20px;
}

.settings-panel--full {
  margin-top: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head--space {
  align-items: center;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--admin-text);
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--admin-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.panel-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.field-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 18px;
  align-items: start;
}

.field-layout--stack {
  grid-template-columns: 1fr;
}

.field-main {
  min-width: 0;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--admin-text);
}

.image-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.image-input-row :deep(.el-input) {
  flex: 1;
}

.field-hint {
  margin: 10px 0 0;
  color: var(--admin-text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.field-hint--compact {
  margin-top: 8px;
}

.preview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-card--wide {
  max-width: 100%;
}

.preview-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--admin-text-soft);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.image-preview {
  border: 1px solid var(--admin-border);
  border-radius: 18px;
  overflow: hidden;
  background: #eef2ff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.image-preview img {
  display: block;
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.image-preview--wide img {
  height: 220px;
}

.preview-placeholder {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  border-radius: 18px;
  border: 1px dashed var(--admin-border);
  background: rgba(255, 255, 255, 0.7);
  color: var(--admin-text-muted);
  text-align: center;
  line-height: 1.8;
}

.preview-placeholder--wide {
  min-height: 220px;
}

.anime-list {
  display: grid;
  gap: 14px;
}

.anime-item {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.7);
}

.anime-preview-shell {
  min-width: 0;
}

.anime-preview img,
.anime-preview--empty {
  width: 100%;
  min-height: 220px;
  height: 220px;
}

.anime-item-body {
  min-width: 0;
}

.anime-item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.anime-title {
  display: block;
  color: var(--admin-text);
  font-weight: 700;
  font-size: 15px;
}

.anime-description {
  margin: 4px 0 0;
  color: var(--admin-text-muted);
  font-size: 12px;
}

.anime-item-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.empty-state {
  padding: 22px 20px;
  border-radius: 18px;
  border: 1px dashed var(--admin-border);
  color: var(--admin-text-muted);
  background: rgba(248, 250, 252, 0.8);
  line-height: 1.8;
}

.save-bar-enter-active,
.save-bar-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.save-bar-enter-from,
.save-bar-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

.save-bar {
  position: sticky;
  bottom: 18px;
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(245, 158, 11, 0.2);
  background: rgba(255, 251, 235, 0.96);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.save-bar-copy strong {
  display: block;
  color: #92400e;
  font-size: 15px;
}

.save-bar-copy p {
  margin: 4px 0 0;
  color: #b45309;
  font-size: 13px;
}

.save-bar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .settings-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .field-layout {
    grid-template-columns: 1fr;
  }

  .settings-hero {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .site-settings {
    padding: 14px;
  }

  .image-input-row,
  .hero-actions,
  .panel-actions,
  .anime-item,
  .anime-item-head,
  .anime-item-actions,
  .save-bar,
  .save-bar-actions {
    display: flex;
    flex-direction: column;
  }

  .image-input-row .el-button,
  .hero-actions .el-button,
  .panel-actions .el-button,
  .anime-item-actions .el-button,
  .save-bar-actions .el-button {
    width: 100%;
  }

  .summary-card strong {
    font-size: 20px;
  }

  .preview-placeholder,
  .anime-preview--empty,
  .anime-preview img,
  .image-preview img,
  .image-preview--wide img {
    min-height: 180px;
    height: 180px;
  }
}
</style>
