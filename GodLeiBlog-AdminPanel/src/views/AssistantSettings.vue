<template>
  <SiteConfigPageShell
    eyebrow="Assistant"
    title="AI 助手"
    description="统一维护馨宝的模型运行配置、前台展示文案与后台私有提示词。前台聊天协议保持不变，API Key 仍只保存在后端。"
    :status-text="statusText"
    :status-description="statusDescription"
    :is-dirty="isDirty"
    :loading="loading"
    :saving="pageSaving"
    :summary-items="summaryItems"
  >
    <template #actions>
      <el-button :loading="loading" @click="reloadAll">重新加载</el-button>
      <el-button @click="openPreview('/')">预览前台</el-button>
      <el-button
        v-if="activeTab === 'runtime'"
        type="primary"
        plain
        :loading="testingRuntime"
        @click="testRuntimeConnection"
      >
        测试连接
      </el-button>
      <el-button
        v-if="activeTab === 'runtime'"
        type="primary"
        :loading="savingRuntime"
        @click="saveRuntimeConfig"
      >
        保存运行配置
      </el-button>
      <el-button
        v-else
        type="primary"
        :loading="savingExperience"
        @click="saveExperienceConfig"
      >
        保存文案与提示词
      </el-button>
    </template>

    <el-tabs v-model="activeTab" class="assistant-settings__tabs">
      <el-tab-pane label="模型运行配置" name="runtime">
        <section class="settings-section">
          <header class="settings-section__head">
            <div>
              <h3>OpenAI-Compatible 运行层</h3>
              <p>这里决定实际调用哪个兼容 `/chat/completions` 的模型供应商、模型名和请求参数。</p>
            </div>
            <span class="assistant-settings__badge">{{ runtimeSourceText }}</span>
          </header>

          <div class="settings-grid settings-grid--two">
            <label class="settings-field">
              <span>供应商预设</span>
              <el-select v-model="runtime.provider" placeholder="选择供应商" @change="handleProviderChange">
                <el-option
                  v-for="preset in providerPresets"
                  :key="preset.provider"
                  :label="preset.label"
                  :value="preset.provider"
                />
              </el-select>
            </label>

            <label class="settings-field">
              <span>模型名</span>
              <el-input v-model="runtime.model" placeholder="deepseek-v4-pro" />
            </label>

            <label class="settings-field settings-field--full">
              <span>Base URL</span>
              <el-input
                v-model="runtime.baseUrl"
                placeholder="例如 https://api.deepseek.com/v1"
              />
              <small class="settings-field__hint">
                预设供应商会自动带默认地址；选择 `custom` 时需要手动填写。
              </small>
            </label>

            <label class="settings-field settings-field--full">
              <span>API Key</span>
              <el-input
                v-model="runtime.apiKey"
                type="password"
                show-password
                clearable
                placeholder="输入新的 API Key；留空则继续使用当前已保存 Key"
              />
            </label>
          </div>

          <div class="assistant-settings__runtime-cards">
            <div class="settings-runtime__card settings-runtime__item">
              <span>当前 Key 状态</span>
              <strong>{{ runtimeMeta.apiKeyConfigured ? '已配置' : '未配置' }}</strong>
              <p class="settings-runtime__hint">{{ runtimeMeta.apiKeyMasked || '当前没有已保存的 Key' }}</p>
            </div>

            <div class="settings-runtime__card settings-runtime__item">
              <span>Key 来源</span>
              <strong>{{ apiKeySourceText }}</strong>
              <p class="settings-runtime__hint">后台已保存的 Key 会优先于环境变量生效。</p>
            </div>

            <div class="settings-runtime__card settings-runtime__item">
              <span>当前生效模型</span>
              <strong>{{ runtime.model || '未配置' }}</strong>
              <p class="settings-runtime__hint">{{ currentProviderLabel }}</p>
            </div>
          </div>

          <div class="settings-inline settings-inline--actions">
            <el-button
              type="danger"
              plain
              :disabled="!runtimeMeta.apiKeyConfigured"
              :loading="savingRuntime"
              @click="clearStoredApiKey"
            >
              清空已保存 Key
            </el-button>
          </div>

          <el-collapse v-model="advancedPanels" class="assistant-settings__collapse">
            <el-collapse-item name="advanced" title="高级参数">
              <div class="settings-grid settings-grid--two assistant-settings__advanced-grid">
                <label class="settings-field">
                  <span>temperature</span>
                  <el-input-number v-model="runtime.temperature" :min="0" :max="2" :step="0.1" :precision="2" />
                </label>

                <label class="settings-field">
                  <span>topP</span>
                  <el-input-number v-model="runtime.topP" :min="0" :max="1" :step="0.05" :precision="2" />
                </label>

                <label class="settings-field">
                  <span>maxTokens</span>
                  <el-input-number v-model="runtime.maxTokens" :min="1" :step="256" />
                </label>

                <label class="settings-field">
                  <span>每分钟限流</span>
                  <el-input-number v-model="runtime.maxRequestsPerMinute" :min="1" :step="1" />
                </label>

                <label class="settings-field">
                  <span>连接超时（ms）</span>
                  <el-input-number v-model="runtime.connectTimeoutMs" :min="1000" :step="1000" />
                </label>

                <label class="settings-field">
                  <span>读取超时（ms）</span>
                  <el-input-number v-model="runtime.readTimeoutMs" :min="1000" :step="5000" />
                </label>
              </div>
            </el-collapse-item>
          </el-collapse>

          <div v-if="runtimeTestResult" class="assistant-settings__test-result" :class="{ 'is-success': runtimeTestResult.success, 'is-fail': !runtimeTestResult.success }">
            <strong>{{ runtimeTestResult.success ? '连接成功' : '连接失败' }}</strong>
            <p>{{ runtimeTestResult.message }}</p>
            <small>耗时 {{ runtimeTestResult.durationMs }}ms</small>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="前台文案与提示词" name="experience">
        <section class="settings-section">
          <header class="settings-section__head">
            <div>
              <h3>前台文案与后台私有提示词</h3>
              <p>systemPrompt 只在后台保存并参与对话链路，前台预览卡不会显示这部分内容。</p>
            </div>
          </header>

          <div class="settings-grid settings-grid--two">
            <label class="settings-field">
              <span>启用助手</span>
              <el-switch v-model="experience.enabled" />
            </label>

            <label class="settings-field">
              <span>助手名称</span>
              <el-input v-model="experience.name" placeholder="馨宝" />
            </label>

            <label class="settings-field settings-field--full">
              <span>副标题</span>
              <el-input v-model="experience.subtitle" placeholder="站内 AI 助手" />
            </label>

            <label class="settings-field settings-field--full">
              <span>欢迎语（Markdown）</span>
              <el-input v-model="experience.welcomeMessage" type="textarea" :rows="5" />
            </label>

            <label class="settings-field settings-field--full">
              <span>免责声明（Markdown）</span>
              <el-input v-model="experience.disclaimer" type="textarea" :rows="4" />
            </label>

            <label class="settings-field settings-field--full">
              <span>systemPrompt</span>
              <el-input v-model="experience.systemPrompt" type="textarea" :rows="8" />
              <small class="settings-field__hint">这部分只会保存在后台私有配置中，不会进入公开的 /site/config。</small>
            </label>
          </div>

          <div class="settings-array">
            <div class="settings-array__head">
              <div>
                <h4>起手提示</h4>
                <p>前台会把这些问题作为快捷入口展示，支持全部清空后继续保存。</p>
              </div>
              <el-button @click="addStarterPrompt">新增问题</el-button>
            </div>

            <div class="settings-array__list settings-array__list--starter">
              <div
                v-for="(item, index) in experience.starterPrompts"
                :key="`starter-${index}`"
                class="settings-array__item settings-array__item--starter"
              >
                <el-input v-model="experience.starterPrompts[index]" placeholder="输入快捷提问" />
                <el-button type="danger" plain @click="removeStarterPrompt(index)">删除</el-button>
              </div>
            </div>

            <p v-if="!experience.starterPrompts.length" class="settings-array__empty">
              当前已清空起手提示，保存后前台将不再显示快捷提问入口。
            </p>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <template #preview>
      <div class="settings-preview__card">
        <span class="settings-preview__eyebrow">Runtime Snapshot</span>
        <h3>当前运行状态</h3>
        <div class="settings-runtime">
          <div class="settings-runtime__grid">
            <div class="settings-runtime__card settings-runtime__item">
              <span>服务开关</span>
              <strong>{{ runtimeMeta.enabled ? '已启用' : '已关闭' }}</strong>
            </div>
            <div class="settings-runtime__card settings-runtime__item">
              <span>供应商</span>
              <strong>{{ currentProviderLabel }}</strong>
            </div>
            <div class="settings-runtime__card settings-runtime__item">
              <span>模型名</span>
              <strong>{{ runtime.model || '未配置' }}</strong>
            </div>
            <div class="settings-runtime__card settings-runtime__item">
              <span>限流</span>
              <strong>{{ runtime.maxRequestsPerMinute || 0 }} / 分钟</strong>
            </div>
          </div>

          <div class="settings-runtime__card settings-runtime__item">
            <span>Base URL</span>
            <strong>{{ runtime.baseUrl || '未配置' }}</strong>
          </div>
        </div>
      </div>

      <div class="settings-preview__card">
        <span class="settings-preview__eyebrow">Assistant Preview</span>
        <div class="settings-preview__assistant" :class="{ 'is-disabled': !experience.enabled }">
          <div class="settings-preview__assistant-head">
            <strong>{{ experience.name || '馨宝' }}</strong>
            <span>{{ experience.enabled ? '已启用' : '已关闭' }}</span>
          </div>

          <p class="assistant-settings__preview-subtitle">{{ experience.subtitle || '站内 AI 助手' }}</p>

          <div
            v-if="hasWelcomeMessage"
            class="settings-preview__markdown"
            v-html="renderMarkdown(experience.welcomeMessage)"
          ></div>

          <div v-if="experience.starterPrompts.length" class="settings-preview__starter-list">
            <span v-for="prompt in experience.starterPrompts.slice(0, 3)" :key="prompt">{{ prompt }}</span>
          </div>

          <div
            v-if="hasDisclaimer"
            class="settings-preview__markdown settings-preview__markdown--muted"
            v-html="renderMarkdown(experience.disclaimer)"
          ></div>

          <p v-if="!hasPreviewContent" class="settings-preview__empty">
            当前前台文案已清空，前台只会展示基础对话区。
          </p>
        </div>
      </div>
    </template>
  </SiteConfigPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import SiteConfigPageShell from '@/components/SiteConfigPageShell.vue'
import {
  getAssistantExperience,
  getAssistantRuntime,
  testAssistantRuntime,
  updateAssistantApiKey,
  updateAssistantExperience,
  updateAssistantRuntime
} from '@/api'
import { openSitePreviewInNewTab } from '@/utils/sitePreview'
import '@/assets/site-config-section.css'

const PREVIEW_BASE_URL = (import.meta.env.VITE_SITE_PREVIEW_URL || '').trim()
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })

const activeTab = ref('runtime')
const loading = ref(false)
const savingRuntime = ref(false)
const savingExperience = ref(false)
const testingRuntime = ref(false)
const runtimeSnapshot = ref('')
const experienceSnapshot = ref('')
const runtimeTestResult = ref(null)
const advancedPanels = ref([])
const previousProvider = ref('deepseek')

const runtime = reactive(createRuntimeForm())
const experience = reactive(createExperienceForm())
const runtimeMeta = reactive({
  enabled: false,
  providerLabel: 'DeepSeek',
  apiKeyConfigured: false,
  apiKeyMasked: '',
  apiKeySource: 'none',
  runtimeSource: 'default',
  presets: []
})

const pageSaving = computed(() => savingRuntime.value || savingExperience.value)
const hasWelcomeMessage = computed(() => Boolean(String(experience.welcomeMessage || '').trim()))
const hasDisclaimer = computed(() => Boolean(String(experience.disclaimer || '').trim()))
const hasPreviewContent = computed(() => hasWelcomeMessage.value || hasDisclaimer.value || experience.starterPrompts.length > 0)
const providerPresets = computed(() => runtimeMeta.presets || [])
const currentProviderLabel = computed(() => {
  const matched = providerPresets.value.find((item) => item.provider === runtime.provider)
  return matched?.label || runtimeMeta.providerLabel || 'OpenAI Compatible'
})

const runtimeDirty = computed(() => JSON.stringify(buildRuntimeComparable()) !== runtimeSnapshot.value)
const experienceDirty = computed(() => JSON.stringify(buildExperienceComparable()) !== experienceSnapshot.value)
const isDirty = computed(() => runtimeDirty.value || experienceDirty.value)

const statusText = computed(() => {
  if (pageSaving.value) return '保存中'
  if (loading.value) return '加载中'
  if (activeTab.value === 'runtime') {
    return runtimeDirty.value ? '运行配置待保存' : '运行配置已同步'
  }
  return experienceDirty.value ? '文案与提示词待保存' : '文案与提示词已同步'
})

const statusDescription = computed(() => {
  if (pageSaving.value) return '正在写入最新助手配置，请稍候。'
  if (loading.value) return '正在从服务端读取助手配置。'
  if (activeTab.value === 'runtime') {
    return runtimeDirty.value
      ? '当前运行配置有未保存修改，保存后会立即影响后端模型调用。'
      : '当前运行配置已经与服务端保持一致。'
  }
  return experienceDirty.value
    ? '当前前台文案或 systemPrompt 有未保存修改，保存后会立即影响展示与对话行为。'
    : '当前前台文案与私有提示词已经与服务端保持一致。'
})

const summaryItems = computed(() => ([
  { label: '当前供应商', value: currentProviderLabel.value },
  { label: '当前模型', value: runtime.model || '未配置' },
  { label: '前台开关', value: experience.enabled ? '已启用' : '已关闭' },
  { label: '起手提示', value: `${experience.starterPrompts.length} 条` }
]))

const apiKeySourceText = computed(() => {
  if (runtimeMeta.apiKeySource === 'admin') return '后台已保存'
  if (runtimeMeta.apiKeySource === 'env') return '环境变量'
  return '未配置'
})

const runtimeSourceText = computed(() => {
  if (runtimeMeta.runtimeSource === 'admin') return '配置来源：后台保存'
  if (runtimeMeta.runtimeSource === 'env') return '配置来源：环境变量'
  if (runtimeMeta.runtimeSource === 'mixed') return '配置来源：后台 + 环境变量混合'
  if (runtimeMeta.runtimeSource === 'preview') return '配置来源：当前预览值'
  return '配置来源：代码默认值'
})

function createRuntimeForm() {
  return {
    provider: 'deepseek',
    baseUrl: '',
    model: 'deepseek-v4-pro',
    apiKey: '',
    temperature: 0.7,
    topP: 0.95,
    maxTokens: null,
    connectTimeoutMs: 10000,
    readTimeoutMs: 120000,
    maxRequestsPerMinute: 12
  }
}

function createExperienceForm() {
  return {
    enabled: true,
    name: '馨宝',
    subtitle: '站内 AI 助手',
    welcomeMessage: '你好，我是 **馨宝**。\n\n我可以结合当前页面内容，陪你一起梳理文章、动态和站点信息。',
    starterPrompts: [],
    disclaimer: 'AI 回复可能存在误差，请结合页面原文和实际情况自行判断。',
    systemPrompt: '你是 GodLei Blog 的站内 AI 助手“馨宝”。回答时请保持自然、准确、简洁；如果页面上下文不足或事实不确定，要明确说明，不要编造。'
  }
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function normalizeText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizePromptList(list) {
  if (!Array.isArray(list)) return []
  return list.map((item) => normalizeText(item)).filter(Boolean)
}

function buildRuntimeComparable() {
  return {
    provider: runtime.provider,
    baseUrl: normalizeText(runtime.baseUrl),
    model: normalizeText(runtime.model),
    apiKey: normalizeText(runtime.apiKey),
    temperature: runtime.temperature,
    topP: runtime.topP,
    maxTokens: runtime.maxTokens,
    connectTimeoutMs: runtime.connectTimeoutMs,
    readTimeoutMs: runtime.readTimeoutMs,
    maxRequestsPerMinute: runtime.maxRequestsPerMinute
  }
}

function buildExperienceComparable() {
  return {
    enabled: experience.enabled,
    name: normalizeText(experience.name),
    subtitle: normalizeText(experience.subtitle),
    welcomeMessage: String(experience.welcomeMessage || '').trim(),
    starterPrompts: normalizePromptList(experience.starterPrompts),
    disclaimer: String(experience.disclaimer || '').trim(),
    systemPrompt: String(experience.systemPrompt || '').trim()
  }
}

function buildRuntimePayload(extra = {}) {
  const apiKey = normalizeText(runtime.apiKey)
  return {
    provider: runtime.provider,
    baseUrl: normalizeText(runtime.baseUrl),
    model: normalizeText(runtime.model),
    temperature: runtime.temperature,
    topP: runtime.topP,
    maxTokens: runtime.maxTokens,
    connectTimeoutMs: runtime.connectTimeoutMs,
    readTimeoutMs: runtime.readTimeoutMs,
    maxRequestsPerMinute: runtime.maxRequestsPerMinute,
    ...(apiKey ? { apiKey } : {}),
    ...extra
  }
}

function buildExperiencePayload() {
  return {
    enabled: experience.enabled,
    name: normalizeText(experience.name),
    subtitle: normalizeText(experience.subtitle),
    welcomeMessage: String(experience.welcomeMessage || '').trim(),
    starterPrompts: normalizePromptList(experience.starterPrompts),
    disclaimer: String(experience.disclaimer || '').trim(),
    systemPrompt: String(experience.systemPrompt || '').trim()
  }
}

function syncRuntimeSnapshot() {
  runtimeSnapshot.value = JSON.stringify(buildRuntimeComparable())
}

function syncExperienceSnapshot() {
  experienceSnapshot.value = JSON.stringify(buildExperienceComparable())
}

function assignRuntime(data = {}) {
  runtime.provider = data.provider || 'deepseek'
  runtime.baseUrl = data.baseUrl || ''
  runtime.model = data.model || 'deepseek-v4-pro'
  runtime.apiKey = ''
  runtime.temperature = data.temperature ?? 0.7
  runtime.topP = data.topP ?? 0.95
  runtime.maxTokens = data.maxTokens ?? null
  runtime.connectTimeoutMs = data.connectTimeoutMs ?? 10000
  runtime.readTimeoutMs = data.readTimeoutMs ?? 120000
  runtime.maxRequestsPerMinute = data.maxRequestsPerMinute ?? 12

  runtimeMeta.enabled = Boolean(data.enabled)
  runtimeMeta.providerLabel = data.providerLabel || 'OpenAI Compatible'
  runtimeMeta.apiKeyConfigured = Boolean(data.apiKeyConfigured)
  runtimeMeta.apiKeyMasked = data.apiKeyMasked || ''
  runtimeMeta.apiKeySource = data.apiKeySource || 'none'
  runtimeMeta.runtimeSource = data.runtimeSource || 'default'
  runtimeMeta.presets = Array.isArray(data.presets) ? clone(data.presets) : []
  previousProvider.value = runtime.provider
  syncRuntimeSnapshot()
}

function assignExperience(data = {}) {
  experience.enabled = data.enabled !== false
  experience.name = data.name || '馨宝'
  experience.subtitle = data.subtitle || '站内 AI 助手'
  experience.welcomeMessage = data.welcomeMessage || ''
  experience.starterPrompts = Array.isArray(data.starterPrompts) ? clone(data.starterPrompts) : []
  experience.disclaimer = data.disclaimer || ''
  experience.systemPrompt = data.systemPrompt || ''
  syncExperienceSnapshot()
}

async function loadRuntime() {
  const data = await getAssistantRuntime()
  assignRuntime(data)
}

async function loadExperience() {
  const data = await getAssistantExperience()
  assignExperience(data)
}

async function reloadAll() {
  loading.value = true
  runtimeTestResult.value = null
  try {
    await Promise.all([loadRuntime(), loadExperience()])
  } catch (error) {
    console.error('加载 AI 助手配置失败', error)
    ElMessage.error(error.message || '加载 AI 助手配置失败')
  } finally {
    loading.value = false
  }
}

async function saveRuntimeConfig() {
  savingRuntime.value = true
  try {
    const data = await updateAssistantRuntime(buildRuntimePayload())
    assignRuntime(data)
    runtimeTestResult.value = null
    ElMessage.success('运行配置已保存')
  } catch (error) {
    console.error('保存运行配置失败', error)
    ElMessage.error(error.message || '保存运行配置失败')
  } finally {
    savingRuntime.value = false
  }
}

async function saveExperienceConfig() {
  savingExperience.value = true
  try {
    const data = await updateAssistantExperience(buildExperiencePayload())
    assignExperience(data)
    ElMessage.success('前台文案与提示词已保存')
  } catch (error) {
    console.error('保存前台文案与提示词失败', error)
    ElMessage.error(error.message || '保存前台文案与提示词失败')
  } finally {
    savingExperience.value = false
  }
}

async function clearStoredApiKey() {
  savingRuntime.value = true
  try {
    const data = await updateAssistantApiKey({
      apiKey: '',
      clearExisting: true
    })
    runtime.apiKey = ''
    assignRuntime(data)
    runtimeTestResult.value = null
    ElMessage.success('已清空后台已保存的 Key')
  } catch (error) {
    console.error('清空 API Key 失败', error)
    ElMessage.error(error.message || '清空 API Key 失败')
  } finally {
    savingRuntime.value = false
  }
}

async function testRuntimeConnection() {
  testingRuntime.value = true
  runtimeTestResult.value = null
  try {
    const result = await testAssistantRuntime(buildRuntimePayload())
    runtimeTestResult.value = result
    if (result.success) {
      ElMessage.success('模型连接测试通过')
    } else {
      ElMessage.warning(result.message || '模型连接测试未通过')
    }
  } catch (error) {
    console.error('测试连接失败', error)
    ElMessage.error(error.message || '测试连接失败')
  } finally {
    testingRuntime.value = false
  }
}

function handleProviderChange(nextProvider) {
  const presets = providerPresets.value
  const previousPreset = presets.find((item) => item.provider === previousProvider.value)
  const nextPreset = presets.find((item) => item.provider === nextProvider)
  const currentBaseUrl = normalizeText(runtime.baseUrl)
  const previousDefaultBaseUrl = previousPreset?.defaultBaseUrl || ''

  if (nextProvider === 'custom') {
    if (!currentBaseUrl || currentBaseUrl === previousDefaultBaseUrl) {
      runtime.baseUrl = ''
    }
  } else if (!currentBaseUrl || currentBaseUrl === previousDefaultBaseUrl) {
    runtime.baseUrl = nextPreset?.defaultBaseUrl || ''
  }

  previousProvider.value = nextProvider
}

function addStarterPrompt() {
  experience.starterPrompts.push('')
}

function removeStarterPrompt(index) {
  experience.starterPrompts.splice(index, 1)
}

function renderMarkdown(content = '') {
  return markdown.render(String(content || ''))
}

function openPreview(path = '/') {
  openSitePreviewInNewTab(path, PREVIEW_BASE_URL)
}

onMounted(() => {
  reloadAll()
})
</script>

<style scoped>
.assistant-settings__tabs {
  display: grid;
  gap: 14px;
}

.assistant-settings__tabs :deep(.el-tabs__header) {
  margin: 0;
}

.assistant-settings__tabs :deep(.el-tabs__nav-wrap::after) {
  background: rgba(214, 173, 92, 0.14);
}

.assistant-settings__tabs :deep(.el-tabs__item) {
  font-weight: 600;
}

.assistant-settings__tabs :deep(.el-tabs__item.is-active) {
  color: #8a6427;
}

.assistant-settings__tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #a94b5d, #d6ad5c);
}

.assistant-settings__badge {
  display: inline-flex;
  padding: 6px 11px;
  border-radius: 999px;
  background: rgba(214, 173, 92, 0.12);
  color: #8a6427;
  font-size: 12px;
  font-weight: 600;
}

.assistant-settings__runtime-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.assistant-settings__collapse {
  margin-top: 14px;
}

.assistant-settings__collapse :deep(.el-collapse-item__header) {
  font-weight: 600;
  color: var(--admin-text);
}

.assistant-settings__collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.assistant-settings__advanced-grid {
  padding-top: 8px;
}

.assistant-settings__test-result {
  display: grid;
  gap: 6px;
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(214, 173, 92, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(250, 246, 238, 0.88));
}

.assistant-settings__test-result strong,
.assistant-settings__test-result p,
.assistant-settings__test-result small {
  margin: 0;
}

.assistant-settings__test-result.is-success {
  border-color: rgba(73, 161, 111, 0.22);
  background: linear-gradient(180deg, rgba(244, 255, 249, 0.98), rgba(235, 248, 240, 0.92));
}

.assistant-settings__test-result.is-fail {
  border-color: rgba(214, 92, 92, 0.18);
  background: linear-gradient(180deg, rgba(255, 248, 248, 0.98), rgba(252, 240, 239, 0.92));
}

.assistant-settings__preview-subtitle {
  margin: -2px 0 2px;
  color: var(--admin-text-soft);
  font-size: 13px;
}

@media (max-width: 900px) {
  .assistant-settings__runtime-cards {
    grid-template-columns: 1fr;
  }
}
</style>
