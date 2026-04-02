<template>
  <SiteConfigPageShell
    eyebrow="Assistant"
    title="AI 助手"
    description="这里同时维护馨宝的展示文案、MiniMax API Key 和运行状态。Key 只保存在后台，不会进入前台站点配置。"
    :status-text="statusText"
    :status-description="statusDescription"
    :is-dirty="isDirty"
    :loading="loading"
    :saving="pageSaving"
    :summary-items="summaryItems"
  >
    <template #actions>
      <el-button @click="reloadAll" :loading="loading || loadingStatus || savingApiKey">重新加载</el-button>
      <el-button @click="openPreview('/')">预览前台</el-button>
      <el-button type="primary" :loading="pageSaving" @click="saveAll">保存助手配置</el-button>
    </template>

    <section class="settings-section">
      <header class="settings-section__head">
        <div>
          <h3>MiniMax 连接配置</h3>
          <p>API Key 支持直接在后台保存。页面只会显示状态和脱敏摘要，不会回显明文。</p>
        </div>
      </header>

      <div class="settings-grid settings-grid--two">
        <label class="settings-field settings-field--full">
          <span>MiniMax API Key</span>
          <el-input
            v-model="assistantApiKey"
            type="password"
            show-password
            clearable
            placeholder="输入新的 MiniMax API Key；留空则保持当前已保存的 Key"
          />
        </label>

        <div class="settings-runtime__card settings-runtime__item">
          <span>当前 Key 状态</span>
          <strong>{{ runtimeStatus.apiKeyConfigured ? '已配置' : '未配置' }}</strong>
          <p class="settings-runtime__hint">{{ runtimeStatus.apiKeyMasked || '当前没有已保存的 Key' }}</p>
        </div>

        <div class="settings-runtime__card settings-runtime__item">
          <span>Key 来源</span>
          <strong>{{ apiKeySourceText }}</strong>
          <p class="settings-runtime__hint">后台已保存 Key 时，会优先于环境变量使用。</p>
        </div>
      </div>

      <div class="settings-inline settings-inline--actions">
        <el-button type="primary" plain :loading="savingApiKey" @click="saveApiKeyOnly">仅保存 API Key</el-button>
        <el-button
          type="danger"
          plain
          :disabled="runtimeStatus.apiKeySource !== 'admin'"
          :loading="savingApiKey"
          @click="clearStoredApiKey"
        >
          清空后台已保存的 Key
        </el-button>
      </div>
    </section>

    <section class="settings-section">
      <header class="settings-section__head">
        <div>
          <h3>展示配置</h3>
          <p>这里只维护前台展示文案和系统提示词，不包含任何敏感密钥。</p>
        </div>
      </header>

      <div class="settings-grid settings-grid--two">
        <label class="settings-field">
          <span>启用助手</span>
          <el-switch v-model="form.assistant.enabled" />
        </label>

        <label class="settings-field">
          <span>助手名称</span>
          <el-input v-model="form.assistant.name" placeholder="馨宝" />
        </label>

        <label class="settings-field settings-field--full">
          <span>副标题</span>
          <el-input v-model="form.assistant.subtitle" placeholder="站内 AI 助手" />
        </label>

        <label class="settings-field settings-field--full">
          <span>欢迎语（Markdown）</span>
          <el-input v-model="form.assistant.welcomeMessage" type="textarea" :rows="5" />
        </label>

        <label class="settings-field settings-field--full">
          <span>免责声明（Markdown）</span>
          <el-input v-model="form.assistant.disclaimer" type="textarea" :rows="4" />
        </label>

        <label class="settings-field settings-field--full">
          <span>系统提示词</span>
          <el-input v-model="form.assistant.systemPrompt" type="textarea" :rows="6" />
        </label>
      </div>

      <div class="settings-array">
        <div class="settings-array__head">
          <div>
            <h4>起手提示</h4>
            <p>前台会把这些问题作为快捷入口展示。</p>
          </div>
          <el-button @click="addTextItem(form.assistant.starterPrompts)">新增问题</el-button>
        </div>
        <div class="settings-array__list settings-array__list--starter">
          <div
            v-for="(item, index) in form.assistant.starterPrompts"
            :key="`starter-${index}`"
            class="settings-array__item settings-array__item--starter"
          >
            <el-input v-model="form.assistant.starterPrompts[index]" placeholder="输入快捷提问" />
            <el-button type="danger" plain @click="removeTextItem(form.assistant.starterPrompts, index)">删除</el-button>
          </div>
        </div>
      </div>
    </section>

    <template #preview>
      <div class="settings-preview__card">
        <span class="settings-preview__eyebrow">Runtime Status</span>
        <h3>服务端状态</h3>
        <div class="settings-runtime">
          <div class="settings-runtime__grid">
            <div class="settings-runtime__card settings-runtime__item">
              <span>服务开关</span>
              <strong>{{ runtimeStatus.enabled ? '已启用' : '已关闭' }}</strong>
            </div>
            <div class="settings-runtime__card settings-runtime__item">
              <span>API Key</span>
              <strong>{{ runtimeStatus.apiKeyConfigured ? '已配置' : '未配置' }}</strong>
              <p class="settings-runtime__hint">{{ runtimeStatus.apiKeyMasked || '暂无已配置 Key' }}</p>
            </div>
            <div class="settings-runtime__card settings-runtime__item">
              <span>模型</span>
              <strong>{{ runtimeStatus.model || 'MiniMax-M2.7' }}</strong>
            </div>
            <div class="settings-runtime__card settings-runtime__item">
              <span>每分钟限流</span>
              <strong>{{ runtimeStatus.maxRequestsPerMinute }}</strong>
            </div>
          </div>
          <div class="settings-runtime__card settings-runtime__item">
            <span>Base URL</span>
            <strong>{{ runtimeStatus.baseUrl || '未配置' }}</strong>
          </div>
          <div class="settings-runtime__card settings-runtime__item">
            <span>Key 来源</span>
            <strong>{{ apiKeySourceText }}</strong>
          </div>
          <span class="settings-runtime__badge" v-if="loadingStatus">状态加载中</span>
        </div>
      </div>

      <div class="settings-preview__card">
        <span class="settings-preview__eyebrow">Assistant Preview</span>
        <div class="settings-preview__assistant" :class="{ 'is-disabled': !form.assistant.enabled }">
          <div class="settings-preview__assistant-head">
            <strong>{{ form.assistant.name || '馨宝' }}</strong>
            <span>{{ form.assistant.enabled ? '已启用' : '已关闭' }}</span>
          </div>
          <div class="settings-preview__markdown" v-html="renderMarkdown(form.assistant.welcomeMessage)"></div>
          <div class="settings-preview__starter-list">
            <span v-for="prompt in form.assistant.starterPrompts.slice(0, 3)" :key="prompt">{{ prompt }}</span>
          </div>
          <div class="settings-preview__markdown settings-preview__markdown--muted" v-html="renderMarkdown(form.assistant.disclaimer)"></div>
        </div>
      </div>
    </template>
  </SiteConfigPageShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SiteConfigPageShell from '@/components/SiteConfigPageShell.vue'
import { getAssistantStatus, updateAssistantApiKey } from '@/api'
import { useSiteConfigSection } from '@/composables/useSiteConfigSection'
import '@/assets/site-config-section.css'

const {
  form,
  loading,
  saving,
  isDirty,
  statusText,
  statusDescription,
  renderMarkdown,
  loadConfig,
  saveSection,
  openPreview,
  addTextItem,
  removeTextItem
} = useSiteConfigSection('assistant')

const loadingStatus = ref(false)
const savingApiKey = ref(false)
const assistantApiKey = ref('')
const runtimeStatus = ref({
  enabled: false,
  model: 'MiniMax-M2.7',
  apiKeyConfigured: false,
  apiKeyMasked: '',
  apiKeySource: 'none',
  baseUrl: '',
  maxRequestsPerMinute: 0
})

const pageSaving = computed(() => saving.value || savingApiKey.value)
const hasPendingApiKey = computed(() => Boolean(String(assistantApiKey.value || '').trim()))
const apiKeySourceText = computed(() => {
  if (runtimeStatus.value.apiKeySource === 'admin') return '后台已保存'
  if (runtimeStatus.value.apiKeySource === 'env') return '环境变量'
  return '未配置'
})

const summaryItems = computed(() => ([
  { label: '前台开关', value: form.assistant.enabled ? '已启用' : '已关闭' },
  { label: '快捷提问', value: `${form.assistant.starterPrompts.length} 条` },
  { label: 'Key 状态', value: runtimeStatus.value.apiKeyConfigured ? '已配置' : '未配置' },
  { label: 'Key 来源', value: apiKeySourceText.value }
]))

async function loadRuntimeStatus() {
  loadingStatus.value = true
  try {
    runtimeStatus.value = await getAssistantStatus()
  } catch (error) {
    console.error('加载 AI 助手运行状态失败', error)
    ElMessage.error(error.message || '加载 AI 助手运行状态失败')
  } finally {
    loadingStatus.value = false
  }
}

async function persistApiKey({ clearExisting = false, silentNoop = false } = {}) {
  const apiKey = String(assistantApiKey.value || '').trim()
  if (!clearExisting && !apiKey) {
    if (!silentNoop) {
      ElMessage.info('请先输入要保存的 MiniMax API Key')
    }
    return false
  }

  savingApiKey.value = true
  try {
    runtimeStatus.value = await updateAssistantApiKey({
      apiKey,
      clearExisting
    })
    assistantApiKey.value = ''
    ElMessage.success(clearExisting ? '已清空后台已保存的 Key' : 'MiniMax API Key 已保存')
    return true
  } catch (error) {
    console.error('保存 MiniMax API Key 失败', error)
    ElMessage.error(error.message || '保存 MiniMax API Key 失败')
    return false
  } finally {
    savingApiKey.value = false
  }
}

async function saveApiKeyOnly() {
  await persistApiKey()
}

async function clearStoredApiKey() {
  await persistApiKey({ clearExisting: true })
}

async function saveAll() {
  await saveSection()
  if (hasPendingApiKey.value) {
    await persistApiKey({ silentNoop: true })
    return
  }
  await loadRuntimeStatus()
}

async function reloadAll() {
  await Promise.all([
    loadConfig(),
    loadRuntimeStatus()
  ])
}

onMounted(() => {
  reloadAll()
})
</script>
