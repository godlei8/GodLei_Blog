<template>
  <SiteConfigPageShell
    eyebrow="Home"
    title="首页管理"
    description="把首页欢迎区、公告区和社交入口拆出来单独维护，保存后前台首页会立即读取。"
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
      <el-button type="primary" :loading="saving" @click="saveSection">保存首页</el-button>
    </template>

    <section class="settings-section">
      <header class="settings-section__head">
        <div>
          <h3>欢迎区</h3>
          <p>背景图、标题和简介文案会直接影响首页首屏呈现。</p>
        </div>
      </header>

      <div class="settings-grid settings-grid--two">
        <label class="settings-field settings-field--full">
          <span>背景图</span>
          <div class="settings-inline">
            <el-input v-model="form.home.backgroundImage" placeholder="输入首页背景图地址" clearable />
            <el-button @click="uploadField('home.backgroundImage', 'home-background')">上传背景图</el-button>
          </div>
          <small class="settings-field__hint">建议优先选择宽幅人物或场景图，修改后可直接在下面预览首屏效果。</small>
        </label>

        <div class="settings-preview__card settings-preview__card--inline settings-field--full">
          <span class="settings-preview__eyebrow">Home Preview</span>
          <h3>{{ form.basic.siteName || 'GodLei Blog' }}</h3>
          <div class="settings-preview__hero" :style="{ backgroundImage: `url(${previewUrl(form.home.backgroundImage)})` }">
            <span>{{ form.home.welcomePrefix || 'WELCOME TO' }}</span>
            <strong>{{ form.home.welcomeHighlight || 'GODLEI BLOG' }}</strong>
          </div>
        </div>

        <label class="settings-field">
          <span>欢迎前缀</span>
          <el-input v-model="form.home.welcomePrefix" placeholder="WELCOME TO" />
        </label>

        <label class="settings-field">
          <span>高亮标题</span>
          <el-input v-model="form.home.welcomeHighlight" placeholder="GODLEI BLOG" />
        </label>
      </div>

      <div class="settings-array">
        <div class="settings-array__head">
          <div>
            <h4>首页简介</h4>
            <p>逐条展示在首页欢迎区。</p>
          </div>
          <el-button @click="addTextItem(form.home.introLines)">新增一行</el-button>
        </div>
        <div class="settings-array__list settings-array__list--compact">
          <div
            v-for="(item, index) in form.home.introLines"
            :key="`intro-${index}`"
            class="settings-array__item settings-array__item--compact"
          >
            <el-input v-model="form.home.introLines[index]" placeholder="输入简介文案" />
            <el-button type="danger" plain @click="removeTextItem(form.home.introLines, index)">删除</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="settings-section">
      <header class="settings-section__head">
        <div>
          <h3>公告与社交入口</h3>
          <p>把首页公告标题、公告条目和社交链接集中维护，减少来回滚动。</p>
        </div>
      </header>

      <div class="settings-home-layout">
        <div class="settings-subsection-card">
          <div class="settings-subsection-card__head">
            <div>
              <h4>公告配置</h4>
              <p>标题和公告条目会显示在首页公告卡片中。</p>
            </div>
          </div>

          <label class="settings-field">
            <span>公告标题</span>
            <el-input v-model="form.home.noticeTitle" placeholder="例如：公告 / Notice" />
          </label>

          <div class="settings-preview__notice-mini">
            <strong>{{ form.home.noticeTitle || '公告' }}</strong>
            <ul>
              <li v-for="(item, index) in form.home.noticeLines.slice(0, 3)" :key="`notice-mini-${index}`">
                {{ item || `公告内容 ${index + 1}` }}
              </li>
            </ul>
          </div>

          <div class="settings-array">
            <div class="settings-array__head">
              <div>
                <h4>公告内容</h4>
                <p>逐条展示在首页公告区域。</p>
              </div>
              <el-button @click="addTextItem(form.home.noticeLines)">新增一行</el-button>
            </div>
            <div class="settings-array__list settings-array__list--compact">
              <div
                v-for="(item, index) in form.home.noticeLines"
                :key="`notice-${index}`"
                class="settings-array__item settings-array__item--compact"
              >
                <el-input v-model="form.home.noticeLines[index]" placeholder="输入公告内容" />
                <el-button type="danger" plain @click="removeTextItem(form.home.noticeLines, index)">删除</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="settings-subsection-card">
          <div class="settings-subsection-card__head">
            <div>
              <h4>社交链接</h4>
              <p>支持名称、图标类名和跳转地址。</p>
            </div>
            <el-button @click="addSocialLink">新增链接</el-button>
          </div>

          <div class="settings-array__list settings-array__list--social">
            <div
              v-for="(item, index) in form.home.socialLinks"
              :key="`social-${index}`"
              class="settings-social-item settings-social-item--stacked"
            >
              <el-input v-model="item.name" placeholder="名称" />
              <el-input v-model="item.icon" placeholder="图标类名，如 fab fa-github" />
              <el-input v-model="item.url" placeholder="链接地址" />
              <el-button type="danger" plain @click="removeTextItem(form.home.socialLinks, index)">删除</el-button>
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
  addTextItem,
  removeTextItem,
  addSocialLink,
  uploadField,
} = useSiteConfigSection('home')

const summaryItems = computed(() => ([
  { label: '欢迎标题', value: form.home.welcomeHighlight || '未设置' },
  { label: '公告条目', value: `${form.home.noticeLines.length} 条` },
  { label: '社交入口', value: `${form.home.socialLinks.length} 个` },
]))

onMounted(() => {
  loadConfig()
})
</script>
