import { computed, reactive, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import { getSiteConfig, saveSiteConfig, uploadImage } from '@/api'
import { openSitePreviewInNewTab } from '@/utils/sitePreview'

const SITE_CONFIG_UPDATE_KEY = 'godlei-site-config-updated-at'
const PREVIEW_BASE_URL = (import.meta.env.VITE_SITE_PREVIEW_URL || '').trim()
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })

const SECTION_LABELS = {
  basic: '站点配置',
  home: '首页管理',
  about: '关于管理',
  assistant: 'AI 助手'
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function formatNow() {
  return new Date().toLocaleString('zh-CN', { hour12: false })
}

function normalizeText(value, fallback = '') {
  const normalized = typeof value === 'string' ? value.trim() : ''
  return normalized || fallback
}

function normalizeOptionalText(value, fallback = '') {
  if (value === null || value === undefined) {
    return fallback
  }
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeBoolean(value, fallback = false) {
  return typeof value === 'boolean' ? value : fallback
}

function normalizeMediaUrl(value) {
  const source = normalizeText(value)
  if (!source) return ''
  if (source.startsWith('/api/uploads/')) return source
  if (source.startsWith('/uploads/')) return `/api${source}`
  if (source.startsWith('uploads/')) return `/api/${source}`
  return source
}

function normalizeTextList(list = []) {
  if (!Array.isArray(list)) return []
  return list.map((item) => normalizeText(item)).filter(Boolean)
}

function normalizeLinkList(list = []) {
  if (!Array.isArray(list)) return []
  return list
    .map((item) => ({
      name: normalizeText(item?.name),
      icon: normalizeText(item?.icon),
      url: normalizeText(item?.url)
    }))
    .filter((item) => item.name || item.url)
}

function createDefaultConfig() {
  return {
    basic: {
      siteName: 'GodLei Blog',
      profileAvatar: ''
    },
    home: {
      backgroundImage: '',
      welcomePrefix: 'WELCOME TO',
      welcomeHighlight: 'GODLEI BLOG',
      noticeTitle: '公告',
      introLines: [],
      noticeLines: [],
      socialLinks: []
    },
    about: {
      animeImages: []
    },
    assistant: {
      enabled: true,
      name: '馨宝',
      subtitle: '站内 AI 助手',
      welcomeMessage: '你好，我是 **馨宝**。\n\n我会结合当前页面内容，帮你梳理文章、动态和站点信息。',
      systemPrompt: '你是 GodLei Blog 的站内 AI 助手“馨宝”。回答时保持自然、准确、简洁；如果页面上下文不足或事实不确定，要明确说明，不要编造。',
      starterPrompts: [
        '帮我总结一下这页内容',
        '这页最值得关注的重点是什么？',
        '如果继续深入，我应该追问什么？'
      ],
      disclaimer: 'AI 回复可能存在误差，请结合页面原文和实际情况自行判断。'
    }
  }
}

function mergeConfig(source = {}) {
  const defaults = createDefaultConfig()
  return {
    basic: {
      siteName: normalizeText(source.basic?.siteName, defaults.basic.siteName),
      profileAvatar: normalizeMediaUrl(source.basic?.profileAvatar || defaults.basic.profileAvatar)
    },
    home: {
      backgroundImage: normalizeMediaUrl(source.home?.backgroundImage || defaults.home.backgroundImage),
      welcomePrefix: normalizeText(source.home?.welcomePrefix, defaults.home.welcomePrefix),
      welcomeHighlight: normalizeText(source.home?.welcomeHighlight, defaults.home.welcomeHighlight),
      noticeTitle: normalizeText(source.home?.noticeTitle, defaults.home.noticeTitle),
      introLines: normalizeTextList(source.home?.introLines || defaults.home.introLines),
      noticeLines: normalizeTextList(source.home?.noticeLines || defaults.home.noticeLines),
      socialLinks: normalizeLinkList(source.home?.socialLinks || defaults.home.socialLinks)
    },
    about: {
      animeImages: normalizeTextList(source.about?.animeImages || defaults.about.animeImages).map(normalizeMediaUrl)
    },
    assistant: {
      enabled: normalizeBoolean(source.assistant?.enabled, defaults.assistant.enabled),
      name: normalizeText(source.assistant?.name, defaults.assistant.name),
      subtitle: normalizeText(source.assistant?.subtitle, defaults.assistant.subtitle),
      welcomeMessage: normalizeOptionalText(source.assistant?.welcomeMessage, defaults.assistant.welcomeMessage),
      systemPrompt: normalizeText(source.assistant?.systemPrompt, defaults.assistant.systemPrompt),
      starterPrompts: normalizeTextList(source.assistant?.starterPrompts || defaults.assistant.starterPrompts),
      disclaimer: normalizeOptionalText(source.assistant?.disclaimer, defaults.assistant.disclaimer)
    }
  }
}

function assignConfig(target, source) {
  Object.assign(target.basic, source.basic)
  Object.assign(target.home, source.home)
  target.home.introLines = [...source.home.introLines]
  target.home.noticeLines = [...source.home.noticeLines]
  target.home.socialLinks = source.home.socialLinks.map((item) => ({ ...item }))
  target.about.animeImages = [...source.about.animeImages]
  Object.assign(target.assistant, source.assistant)
  target.assistant.starterPrompts = [...source.assistant.starterPrompts]
}

function buildPayload(source) {
  return mergeConfig(source)
}

function buildSectionPayload(source, sectionKey) {
  return clone(buildPayload(source)[sectionKey])
}

function notifyFrontendConfigUpdated() {
  if (typeof window === 'undefined' || !window.localStorage) return
  window.localStorage.setItem(SITE_CONFIG_UPDATE_KEY, String(Date.now()))
}

function getValueByPath(target, path) {
  return path.split('.').reduce((current, segment) => current?.[segment], target)
}

function setValueByPath(target, path, value) {
  const segments = path.split('.')
  let current = target
  segments.forEach((segment, index) => {
    if (index === segments.length - 1) {
      current[segment] = value
      return
    }
    current = current[segment]
  })
}

export function useSiteConfigSection(sectionKey) {
  const form = reactive(createDefaultConfig())
  const loading = ref(false)
  const saving = ref(false)
  const lastSyncedAt = ref('')
  const savedSnapshot = ref('')

  const isDirty = computed(() => JSON.stringify(buildSectionPayload(form, sectionKey)) !== savedSnapshot.value)
  const statusText = computed(() => {
    if (saving.value) return '保存中'
    if (loading.value) return '加载中'
    if (isDirty.value) return '待保存'
    return '已同步'
  })
  const statusDescription = computed(() => {
    if (saving.value) return `正在写入${SECTION_LABELS[sectionKey] || '站点配置'}，请稍候。`
    if (loading.value) return '正在读取服务端站点配置。'
    if (isDirty.value) return `当前${SECTION_LABELS[sectionKey] || '站点配置'}有未保存修改，保存后前台会立即读取最新内容。`
    if (lastSyncedAt.value) return `最近同步于 ${lastSyncedAt.value}`
    return '当前配置已经与服务端保持一致。'
  })

  function syncSnapshot() {
    savedSnapshot.value = JSON.stringify(buildSectionPayload(form, sectionKey))
    lastSyncedAt.value = formatNow()
  }

  function previewUrl(url) {
    return normalizeMediaUrl(url)
  }

  function renderMarkdown(content = '') {
    return markdown.render(String(content || ''))
  }

  async function loadConfig() {
    loading.value = true
    try {
      const data = await getSiteConfig()
      assignConfig(form, mergeConfig(data))
      syncSnapshot()
    } catch (error) {
      console.error('加载站点配置失败', error)
      ElMessage.error(error.message || '加载站点配置失败')
      assignConfig(form, createDefaultConfig())
      syncSnapshot()
    } finally {
      loading.value = false
    }
  }

  async function saveSection() {
    saving.value = true
    try {
      const latest = mergeConfig(await getSiteConfig())
      latest[sectionKey] = buildSectionPayload(form, sectionKey)
      await saveSiteConfig(latest)
      assignConfig(form, latest)
      notifyFrontendConfigUpdated()
      syncSnapshot()
      ElMessage.success(`${SECTION_LABELS[sectionKey] || '站点配置'}已保存`)
      return true
    } catch (error) {
      console.error('保存站点配置失败', error)
      ElMessage.error(error.message || '保存站点配置失败')
      return false
    } finally {
      saving.value = false
    }
  }

  function openPreview(path = '/') {
    openSitePreviewInNewTab(path, PREVIEW_BASE_URL)
  }

  function addTextItem(list) {
    list.push('')
  }

  function removeTextItem(list, index) {
    list.splice(index, 1)
  }

  function addSocialLink() {
    form.home.socialLinks.push({ name: '', icon: '', url: '' })
  }

  async function pickAndUpload(bizType) {
    return new Promise((resolve) => {
      const input = document.createElement('input')
      input.type = 'file'
      input.accept = 'image/*'
      input.onchange = async () => {
        const file = input.files?.[0]
        if (!file) {
          resolve(null)
          return
        }

        try {
          const uploaded = await uploadImage(file, bizType)
          resolve(uploaded)
        } catch (error) {
          console.error('上传图片失败', error)
          ElMessage.error(error.message || '上传图片失败')
          resolve(null)
        }
      }
      input.click()
    })
  }

  async function uploadField(path, bizType) {
    const uploaded = await pickAndUpload(bizType)
    if (!uploaded?.url) return
    setValueByPath(form, path, normalizeMediaUrl(uploaded.url))
    await saveSection()
  }

  async function appendUploadedItem(path, bizType) {
    const uploaded = await pickAndUpload(bizType)
    if (!uploaded?.url) return
    const list = getValueByPath(form, path)
    if (!Array.isArray(list)) return
    list.push(normalizeMediaUrl(uploaded.url))
    await saveSection()
  }

  async function replaceUploadedItem(path, index, bizType) {
    const uploaded = await pickAndUpload(bizType)
    if (!uploaded?.url) return
    const list = getValueByPath(form, path)
    if (!Array.isArray(list)) return
    list.splice(index, 1, normalizeMediaUrl(uploaded.url))
    await saveSection()
  }

  return {
    form,
    loading,
    saving,
    lastSyncedAt,
    isDirty,
    statusText,
    statusDescription,
    normalizeMediaUrl,
    previewUrl,
    renderMarkdown,
    loadConfig,
    saveSection,
    openPreview,
    addTextItem,
    removeTextItem,
    addSocialLink,
    setValueByPath: (path, value) => setValueByPath(form, path, value),
    uploadField,
    appendUploadedItem,
    replaceUploadedItem
  }
}
