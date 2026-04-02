<template>
  <div v-if="assistantEnabled" class="assistant-shell" :class="{ 'is-open': open, 'is-mobile': isMobile }">
    <transition name="assistant-trigger">
      <button
        v-if="!open"
        class="assistant-trigger"
        type="button"
        :aria-label="`打开 ${assistantName}`"
        @click="openPanel"
      >
        <div class="assistant-trigger__bubble">
          <span class="assistant-trigger__badge">HELLO</span>
          <div class="assistant-trigger__content">
            <strong class="assistant-trigger__title">我是{{ assistantName }}</strong>
            <small class="assistant-trigger__subtitle">{{ assistantSubtitle }}</small>
            <small class="assistant-trigger__note">点我开始 AI 问答</small>
          </div>
        </div>

        <div class="assistant-trigger__pet" aria-hidden="true">
          <span class="assistant-trigger__shadow"></span>
          <span class="assistant-trigger__tail"></span>
          <span class="assistant-trigger__body"></span>
          <span class="assistant-trigger__belly"></span>
          <span class="assistant-trigger__arm assistant-trigger__arm--left"></span>
          <span class="assistant-trigger__arm assistant-trigger__arm--right"></span>
          <span class="assistant-trigger__leg assistant-trigger__leg--left"></span>
          <span class="assistant-trigger__leg assistant-trigger__leg--right"></span>
          <span class="assistant-trigger__ear assistant-trigger__ear--left"></span>
          <span class="assistant-trigger__ear assistant-trigger__ear--right"></span>
          <div class="assistant-trigger__face">
            <span class="assistant-trigger__eye assistant-trigger__eye--left"></span>
            <span class="assistant-trigger__eye assistant-trigger__eye--right"></span>
            <span class="assistant-trigger__nose"></span>
            <span class="assistant-trigger__mouth"></span>
            <span class="assistant-trigger__whisker assistant-trigger__whisker--left"></span>
            <span class="assistant-trigger__whisker assistant-trigger__whisker--right"></span>
          </div>
          <span class="assistant-trigger__paw assistant-trigger__paw--left"></span>
          <span class="assistant-trigger__paw assistant-trigger__paw--right"></span>
        </div>
      </button>
    </transition>

    <transition name="assistant-mask">
      <div v-if="open && isMobile" class="assistant-mask" @click="closePanel"></div>
    </transition>

    <transition name="assistant-panel">
      <section v-if="open" ref="panelRef" class="assistant-panel" @wheel.stop>
        <header class="assistant-panel__header">
          <div class="assistant-panel__brand">
            <div class="assistant-panel__avatar" aria-hidden="true">
              <span class="assistant-panel__avatar-ear assistant-panel__avatar-ear--left"></span>
              <span class="assistant-panel__avatar-ear assistant-panel__avatar-ear--right"></span>
              <span class="assistant-panel__avatar-eye assistant-panel__avatar-eye--left"></span>
              <span class="assistant-panel__avatar-eye assistant-panel__avatar-eye--right"></span>
              <span class="assistant-panel__avatar-nose"></span>
            </div>

            <div class="assistant-panel__copy">
              <span class="assistant-panel__eyebrow">Cat Assistant</span>
              <h2>{{ assistantName }}</h2>
              <p>{{ assistantSubtitle }}</p>
            </div>
          </div>

          <div class="assistant-panel__actions">
            <button type="button" @click="clearConversation">清空</button>
            <button type="button" @click="closePanel">关闭</button>
          </div>
        </header>

        <div class="assistant-panel__context">
          <span>{{ currentContextLabel }}</span>
        </div>

        <div ref="messagesRef" class="assistant-panel__messages" @wheel.prevent="handleMessagesWheel">
          <div v-if="!messages.length" class="assistant-empty">
            <div class="assistant-empty__welcome assistant-markdown" v-html="welcomeHtml"></div>

            <div v-if="starterPrompts.length" class="assistant-empty__starters">
              <button
                v-for="prompt in starterPrompts"
                :key="prompt"
                type="button"
                @click="submitMessage(prompt)"
              >
                {{ prompt }}
              </button>
            </div>

            <div class="assistant-empty__disclaimer assistant-markdown" v-html="disclaimerHtml"></div>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="assistant-message"
            :class="`is-${message.role}`"
          >
            <div class="assistant-message__meta">
              <strong>{{ message.role === 'user' ? '我' : assistantName }}</strong>
              <span v-if="message.pending">正在回复...</span>
            </div>

            <div
              v-if="message.role === 'assistant'"
              class="assistant-message__body assistant-markdown"
              v-html="message.html"
            ></div>
            <p v-else class="assistant-message__plain">{{ message.content }}</p>
          </article>
        </div>

        <form class="assistant-panel__composer" @submit.prevent="submitMessage()">
          <textarea
            v-model="draftMessage"
            rows="3"
            :placeholder="`想问${assistantName}什么？按 Enter 发送，Shift + Enter 换行`"
            @keydown="handleTextareaKeydown"
          ></textarea>

          <div class="assistant-panel__composer-actions">
            <span v-if="errorMessage" class="assistant-panel__error">{{ errorMessage }}</span>
            <button type="submit" :disabled="submitting || !draftMessage.trim()">
              {{ submitting ? '发送中...' : '发送' }}
            </button>
          </div>
        </form>
      </section>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import plaintext from 'highlight.js/lib/languages/plaintext'
import python from 'highlight.js/lib/languages/python'
import shell from 'highlight.js/lib/languages/shell'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import 'highlight.js/styles/github-dark.css'
import { bindImageFallback, coverFallbackUrl, resolveImageUrl } from '@/utils/image'
import { pageContextState } from '@/utils/pageContext'
import { getDefaultSiteConfig, loadSiteConfig } from '@/utils/siteConfig'

const STORAGE_KEY = 'godlei-assistant-session'
const renderTimers = new Map()

;[
  ['bash', bash],
  ['c', c],
  ['cpp', cpp],
  ['c++', cpp],
  ['css', css],
  ['html', xml],
  ['java', java],
  ['javascript', javascript],
  ['js', javascript],
  ['json', json],
  ['markdown', markdown],
  ['md', markdown],
  ['plaintext', plaintext],
  ['python', python],
  ['py', python],
  ['shell', shell],
  ['sh', shell],
  ['sql', sql],
  ['typescript', typescript],
  ['ts', typescript],
  ['vue', xml],
  ['xml', xml],
  ['yaml', yaml],
  ['yml', yaml],
].forEach(([name, language]) => {
  hljs.registerLanguage(name, language)
})

const md = createMarkdownRenderer()
const open = ref(false)
const isMobile = ref(false)
const submitting = ref(false)
const draftMessage = ref('')
const errorMessage = ref('')
const messages = ref([])
const siteConfig = ref(getDefaultSiteConfig())
const sessionId = ref(createSessionId())
const panelRef = ref(null)
const messagesRef = ref(null)
const requestController = ref(null)

const assistantEnabled = computed(() => siteConfig.value.assistant?.enabled !== false)
const assistantName = computed(() => siteConfig.value.assistant?.name || '馨宝')
const assistantSubtitle = computed(() => siteConfig.value.assistant?.subtitle || '站内 AI 助手')
const starterPrompts = computed(() => siteConfig.value.assistant?.starterPrompts || [])
const welcomeHtml = computed(() => renderMarkdown(siteConfig.value.assistant?.welcomeMessage || ''))
const disclaimerHtml = computed(() => renderMarkdown(siteConfig.value.assistant?.disclaimer || ''))
const currentContextLabel = computed(() => pageContextState.title || '当前页面')

function createSessionId() {
  return `assistant-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
}

function createMessage(role, content = '') {
  return reactive({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    role,
    content,
    html: role === 'assistant' ? renderMarkdown(content) : '',
    pending: false,
  })
}

function createMarkdownRenderer() {
  const instance = new MarkdownIt({
    html: false,
    linkify: true,
    breaks: true,
    highlight(str, lang) {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(str, { language: lang }).value
        } catch (_) {}
      }
      return hljs.highlightAuto(str).value
    },
  })

  instance.renderer.rules.link_open = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    token.attrSet('target', '_blank')
    token.attrSet('rel', 'noopener noreferrer')
    return self.renderToken(tokens, idx, options)
  }

  instance.renderer.rules.image = (tokens, idx) => {
    const token = tokens[idx]
    const src = resolveImageUrl(token.attrGet('src'), coverFallbackUrl)
    const alt = token.content || ''
    return `<img class="assistant-markdown__image" src="${src}" alt="${alt}" loading="lazy" />`
  }

  instance.renderer.rules.fence = (tokens, idx, options) => {
    const token = tokens[idx]
    const info = token.info ? token.info.trim() : ''
    const lang = info.split(/\s+/g)[0] || ''
    const highlighted = options.highlight ? options.highlight(token.content, lang) : ''
    const content = highlighted || instance.utils.escapeHtml(token.content || '')
    const label = lang || 'text'
    return `
      <div class="assistant-code">
        <div class="assistant-code__header">
          <span>${label}</span>
          <button class="assistant-code__copy" type="button">复制代码</button>
        </div>
        <pre class="assistant-code__body"><code class="hljs language-${label.toLowerCase()}">${content}</code></pre>
      </div>
    `
  }

  return instance
}

function renderMarkdown(content = '') {
  return md.render(String(content || ''))
}

function updateViewportState() {
  isMobile.value = window.innerWidth <= 768
}

function openPanel() {
  open.value = true
  nextTick(scrollMessagesToBottom)
}

function closePanel() {
  open.value = false
}

function clearConversation() {
  messages.value = []
  sessionId.value = createSessionId()
  errorMessage.value = ''
  persistMessages()
}

function persistMessages() {
  if (typeof window === 'undefined') return
  const payload = {
    sessionId: sessionId.value,
    messages: messages.value.slice(-20).map((message) => ({
      id: message.id,
      role: message.role,
      content: message.content,
    })),
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
}

function restoreMessages() {
  if (typeof window === 'undefined') return
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return

  try {
    const payload = JSON.parse(raw)
    sessionId.value = payload.sessionId || createSessionId()
    messages.value = Array.isArray(payload.messages)
      ? payload.messages.map((item) => createMessage(item.role, item.content))
      : []
  } catch (error) {
    console.error('恢复馨宝对话失败', error)
  }
}

function serializeMessagesForRequest() {
  return messages.value
    .filter((item) => (item.role === 'user' || item.role === 'assistant') && item.content.trim())
    .slice(-12)
    .map((item) => ({
      role: item.role,
      content: item.content,
    }))
}

async function loadConfig(force = false) {
  try {
    siteConfig.value = await loadSiteConfig(force)
    if (!assistantEnabled.value) {
      open.value = false
    }
  } catch (error) {
    console.error('加载馨宝配置失败', error)
  }
}

async function submitMessage(preset = '') {
  const content = String(preset || draftMessage.value || '').trim()
  if (!content || submitting.value) return

  openPanel()
  errorMessage.value = ''
  draftMessage.value = ''

  const userMessage = createMessage('user', content)
  const assistantMessage = createMessage('assistant', '')
  assistantMessage.pending = true

  messages.value = [...messages.value, userMessage, assistantMessage]
  persistMessages()
  await nextTick(scrollMessagesToBottom)

  submitting.value = true
  const controller = new AbortController()
  requestController.value = controller

  try {
    const response = await fetch('/api/assistant/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        sessionId: sessionId.value,
        messages: serializeMessagesForRequest(),
        pageContext: { ...pageContextState },
      }),
      signal: controller.signal,
    })

    if (!response.ok || !response.body) {
      throw new Error(`请求失败：${response.status}`)
    }

    await consumeSseStream(response.body, {
      delta: (payload) => {
        const chunk = typeof payload === 'string' ? payload : payload?.content || ''
        if (!chunk) return
        assistantMessage.content += chunk
        assistantMessage.pending = false
        scheduleRender(assistantMessage)
      },
      error: (payload) => {
        const message = typeof payload === 'string' ? payload : payload?.message || '请求失败'
        errorMessage.value = message
        assistantMessage.pending = false
        if (!assistantMessage.content) {
          assistantMessage.content = `抱歉，${message}`
          flushRender(assistantMessage)
        }
      },
      done: () => {
        assistantMessage.pending = false
        flushRender(assistantMessage)
      },
    })
  } catch (error) {
    if (error?.name !== 'AbortError') {
      console.error('馨宝请求失败', error)
      errorMessage.value = error?.message || '请求失败'
      assistantMessage.pending = false
      if (!assistantMessage.content) {
        assistantMessage.content = `抱歉，${errorMessage.value}`
        flushRender(assistantMessage)
      }
    }
  } finally {
    assistantMessage.pending = false
    flushRender(assistantMessage)
    requestController.value = null
    submitting.value = false
    persistMessages()
    await nextTick(scrollMessagesToBottom)
  }
}

async function consumeSseStream(stream, handlers) {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex >= 0) {
      const block = buffer.slice(0, separatorIndex)
      buffer = buffer.slice(separatorIndex + 2)
      dispatchSseBlock(block, handlers)
      separatorIndex = buffer.indexOf('\n\n')
    }
  }

  if (buffer.trim()) {
    dispatchSseBlock(buffer, handlers)
  }
}

function dispatchSseBlock(block, handlers) {
  const lines = block.split(/\r?\n/)
  let event = 'message'
  const dataLines = []

  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  })

  const payloadText = dataLines.join('\n')
  if (!payloadText) return

  let payload = payloadText
  try {
    payload = JSON.parse(payloadText)
  } catch (_) {}

  if (handlers[event]) {
    handlers[event](payload)
  }
}

function scheduleRender(message) {
  if (!message) return
  if (renderTimers.has(message.id)) return
  const timer = window.setTimeout(() => {
    flushRender(message)
    renderTimers.delete(message.id)
  }, 120)
  renderTimers.set(message.id, timer)
}

function flushRender(message) {
  if (!message) return
  if (renderTimers.has(message.id)) {
    window.clearTimeout(renderTimers.get(message.id))
    renderTimers.delete(message.id)
  }
  message.html = renderMarkdown(message.content)
  nextTick(() => {
    enhanceAssistantContent()
    scrollMessagesToBottom()
  })
}

function enhanceAssistantContent() {
  const root = panelRef.value
  if (!root) return

  root.querySelectorAll('.assistant-code__copy').forEach((button) => {
    if (button.dataset.bound === 'true') return
    button.dataset.bound = 'true'

    button.addEventListener('click', async () => {
      const code = button.closest('.assistant-code')?.querySelector('code')?.innerText || ''
      if (!code) return
      try {
        await navigator.clipboard.writeText(code)
        const previous = button.innerText
        button.innerText = '已复制'
        window.setTimeout(() => {
          button.innerText = previous
        }, 1800)
      } catch (error) {
        console.warn('复制代码失败', error)
      }
    })
  })

  root.querySelectorAll('.assistant-markdown__image').forEach((img) => {
    bindImageFallback(img, coverFallbackUrl)
  })
}

function scrollMessagesToBottom() {
  const container = messagesRef.value
  if (!container) return
  container.scrollTop = container.scrollHeight
}

function handleMessagesWheel(event) {
  const container = messagesRef.value
  if (!container) return
  const delta = Number(event?.deltaY || 0)
  if (!delta) return
  container.scrollTop += delta
}

function handleTextareaKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submitMessage()
  }
}

function handleStorage() {
  loadConfig(true)
}

function handleFocus() {
  loadConfig(true)
}

watch(
  () => messages.value.length,
  () => {
    persistMessages()
  }
)

onMounted(async () => {
  restoreMessages()
  await loadConfig(true)
  updateViewportState()
  window.addEventListener('resize', updateViewportState)
  window.addEventListener('storage', handleStorage)
  window.addEventListener('focus', handleFocus)
  nextTick(() => {
    messages.value.forEach((message) => {
      if (message.role === 'assistant') {
        flushRender(message)
      }
    })
  })
})

onBeforeUnmount(() => {
  if (requestController.value) {
    requestController.value.abort()
  }
  renderTimers.forEach((timer) => window.clearTimeout(timer))
  window.removeEventListener('resize', updateViewportState)
  window.removeEventListener('storage', handleStorage)
  window.removeEventListener('focus', handleFocus)
})
</script>

<style scoped>
.assistant-shell {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1300;
}

.assistant-trigger {
  width: min(266px, calc(100vw - 40px));
  padding: 0;
  border: 0;
  background: transparent;
  display: grid;
  gap: 10px;
  justify-items: end;
  color: #fff7eb;
  cursor: pointer;
  transition: transform 0.28s ease, filter 0.28s ease;
}

.assistant-trigger:hover {
  transform: translateY(-6px);
  filter: drop-shadow(0 20px 28px rgba(10, 2, 5, 0.22));
}

.assistant-trigger__pet {
  position: relative;
  width: 166px;
  height: 170px;
  justify-self: center;
  margin-right: 18px;
  animation: assistant-float 3.8s ease-in-out infinite;
}

.assistant-trigger__shadow {
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: 114px;
  height: 24px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(10, 2, 5, 0.38), transparent 72%);
  transform: translateX(-50%);
  filter: blur(2px);
}

.assistant-trigger__tail {
  position: absolute;
  right: 6px;
  bottom: 48px;
  width: 48px;
  height: 22px;
  border: 6px solid #e7c089;
  border-left: 0;
  border-radius: 0 24px 24px 0;
  transform-origin: left center;
  animation: assistant-tail 2.4s ease-in-out infinite;
  z-index: 0;
}

.assistant-trigger__body {
  position: absolute;
  left: 36px;
  bottom: 28px;
  width: 92px;
  height: 82px;
  border-radius: 44px 44px 30px 30px;
  background: linear-gradient(180deg, #efc98e, #d7a865 86%);
  box-shadow:
    inset 0 -10px 14px rgba(154, 90, 30, 0.14),
    0 14px 22px rgba(10, 2, 5, 0.18);
}

.assistant-trigger__belly {
  position: absolute;
  left: 52px;
  bottom: 32px;
  width: 62px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(180deg, #fff1da, #f5d7a8 88%);
  z-index: 1;
}

.assistant-trigger__arm,
.assistant-trigger__leg {
  position: absolute;
  background: linear-gradient(180deg, #efc98e, #d7a865 86%);
  box-shadow: inset 0 -6px 8px rgba(154, 90, 30, 0.1);
}

.assistant-trigger__arm {
  bottom: 56px;
  width: 20px;
  height: 62px;
  border-radius: 18px;
  z-index: 2;
}

.assistant-trigger__arm--left {
  left: 26px;
  transform: rotate(10deg);
}

.assistant-trigger__arm--right {
  right: 24px;
  transform: rotate(-10deg);
}

.assistant-trigger__leg {
  bottom: 14px;
  width: 24px;
  height: 48px;
  border-radius: 18px;
  z-index: 1;
}

.assistant-trigger__leg--left {
  left: 54px;
}

.assistant-trigger__leg--right {
  right: 52px;
}

.assistant-trigger__ear {
  position: absolute;
  top: 18px;
  width: 34px;
  height: 34px;
  background: linear-gradient(180deg, #ffe8c7, #efc789);
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
  z-index: 4;
}

.assistant-trigger__ear--left {
  left: 42px;
  transform: rotate(-12deg);
}

.assistant-trigger__ear--right {
  right: 42px;
  transform: rotate(12deg);
}

.assistant-trigger__face {
  position: absolute;
  left: 32px;
  bottom: 72px;
  width: 100px;
  height: 92px;
  border-radius: 50%;
  background: linear-gradient(180deg, #ffeacc, #efc98e 72%);
  box-shadow: inset 0 -8px 12px rgba(161, 95, 29, 0.12), 0 12px 24px rgba(0, 0, 0, 0.14);
  z-index: 3;
}

.assistant-trigger__eye,
.assistant-panel__avatar-eye {
  position: absolute;
  border-radius: 999px;
  background: #3e2114;
  animation: assistant-blink 3.4s infinite;
}

.assistant-trigger__eye {
  top: 38px;
  width: 10px;
  height: 12px;
}

.assistant-trigger__eye--left {
  left: 24px;
}

.assistant-trigger__eye--right {
  right: 24px;
}

.assistant-trigger__nose,
.assistant-panel__avatar-nose {
  position: absolute;
  left: 50%;
  border-radius: 50% 50% 65% 65%;
  background: #ab5a57;
  transform: translateX(-50%);
}

.assistant-trigger__nose {
  top: 52px;
  width: 12px;
  height: 8px;
}

.assistant-trigger__mouth {
  position: absolute;
  left: 50%;
  top: 60px;
  width: 22px;
  height: 10px;
  border-bottom: 2px solid #6f3b2d;
  border-radius: 0 0 18px 18px;
  transform: translateX(-50%);
}

.assistant-trigger__whisker {
  position: absolute;
  top: 56px;
  width: 24px;
  height: 2px;
  background: rgba(108, 62, 44, 0.7);
}

.assistant-trigger__whisker--left {
  left: 0;
  box-shadow: 0 10px 0 rgba(108, 62, 44, 0.7);
  transform: rotate(10deg);
}

.assistant-trigger__whisker--right {
  right: 0;
  box-shadow: 0 10px 0 rgba(108, 62, 44, 0.7);
  transform: rotate(-10deg);
}

.assistant-trigger__paw {
  position: absolute;
  bottom: 8px;
  width: 22px;
  height: 16px;
  border-radius: 999px;
  background: #ffe6c0;
  z-index: 2;
}

.assistant-trigger__paw--left {
  left: 54px;
}

.assistant-trigger__paw--right {
  right: 52px;
}

.assistant-trigger__bubble {
  position: relative;
  width: min(244px, calc(100vw - 56px));
  min-height: 102px;
  padding: 16px 18px;
  border-radius: 24px 24px 18px 24px;
  background: linear-gradient(180deg, rgba(255, 251, 246, 0.96), rgba(247, 230, 204, 0.92));
  color: #352015;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.58),
    0 16px 30px rgba(9, 2, 5, 0.16);
  align-self: end;
  animation: assistant-bob 3.8s ease-in-out infinite;
}

.assistant-trigger__bubble::before {
  content: '';
  position: absolute;
  right: 32px;
  bottom: -12px;
  width: 18px;
  height: 18px;
  background: inherit;
  clip-path: polygon(50% 100%, 0 0, 100% 0);
}

.assistant-trigger__badge {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(122, 29, 45, 0.12);
  color: #7a1d2d;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.assistant-trigger__content {
  display: grid;
  gap: 4px;
  margin-top: 10px;
}

.assistant-trigger__title {
  margin: 0;
  font-size: 1.3rem;
  line-height: 1.08;
}

.assistant-trigger__subtitle {
  font-size: 0.95rem;
  line-height: 1.5;
}

.assistant-trigger__note {
  color: rgba(53, 32, 21, 0.58);
  font-size: 0.82rem;
  line-height: 1.4;
}

.assistant-panel {
  width: min(436px, calc(100vw - 24px));
  height: min(80vh, 760px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  border-radius: 28px;
  border: 1px solid rgba(214, 173, 92, 0.18);
  background:
    radial-gradient(circle at 100% 0%, rgba(214, 173, 92, 0.12), transparent 0 24%),
    linear-gradient(180deg, rgba(18, 7, 11, 0.98), rgba(32, 10, 16, 0.96));
  box-shadow: 0 30px 58px rgba(10, 2, 5, 0.46);
  overflow: hidden;
}

.assistant-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.assistant-panel__brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.assistant-panel__avatar {
  position: relative;
  width: 50px;
  height: 50px;
  flex: 0 0 50px;
  border-radius: 50%;
  background: linear-gradient(180deg, #ffeacc, #efc98e 72%);
}

.assistant-panel__avatar-ear {
  position: absolute;
  top: -8px;
  width: 18px;
  height: 18px;
  background: linear-gradient(180deg, #ffe8c7, #efc789);
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
}

.assistant-panel__avatar-ear--left {
  left: 5px;
  transform: rotate(-12deg);
}

.assistant-panel__avatar-ear--right {
  right: 5px;
  transform: rotate(12deg);
}

.assistant-panel__avatar-eye {
  top: 20px;
  width: 7px;
  height: 7px;
}

.assistant-panel__avatar-eye--left {
  left: 13px;
}

.assistant-panel__avatar-eye--right {
  right: 13px;
}

.assistant-panel__avatar-nose {
  top: 27px;
  width: 8px;
  height: 6px;
}

.assistant-panel__eyebrow {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 247, 234, 0.08);
  color: rgba(255, 239, 217, 0.76);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.assistant-panel__header h2 {
  margin: 10px 0 4px;
  font-size: 24px;
  color: #fff8ef;
}

.assistant-panel__header p,
.assistant-message__meta,
.assistant-panel__context span,
.assistant-panel__error {
  margin: 0;
  color: rgba(255, 247, 234, 0.7);
}

.assistant-trigger__subtitle {
  color: rgba(53, 32, 21, 0.72);
}

.assistant-panel__actions,
.assistant-empty__starters,
.assistant-panel__composer-actions {
  display: flex;
  gap: 10px;
}

.assistant-panel__actions button,
.assistant-panel__composer button,
.assistant-empty__starters button {
  border-radius: 999px;
  cursor: pointer;
}

.assistant-panel__actions button,
.assistant-empty__starters button {
  border: 1px solid rgba(214, 173, 92, 0.2);
  background: rgba(255, 255, 255, 0.04);
  color: var(--theme-accent-text-strong);
  padding: 8px 12px;
}

.assistant-panel__context,
.assistant-panel__messages,
.assistant-panel__composer {
  padding-left: 18px;
  padding-right: 18px;
}

.assistant-panel__context {
  padding-top: 10px;
}

.assistant-panel__context span {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 247, 234, 0.06);
  font-size: 12px;
}

.assistant-panel__messages {
  overflow-y: auto;
  padding-top: 18px;
  padding-bottom: 18px;
  display: grid;
  gap: 14px;
}

.assistant-empty,
.assistant-message,
.assistant-panel__composer,
.assistant-trigger__content {
  display: grid;
}

.assistant-empty {
  gap: 18px;
}

.assistant-message {
  gap: 8px;
}

.assistant-message.is-user {
  justify-items: end;
}

.assistant-message__plain,
.assistant-message__body {
  width: fit-content;
  max-width: 100%;
  margin: 0;
  padding: 14px 16px;
  border-radius: 20px;
  line-height: 1.8;
}

.assistant-message__plain {
  background: linear-gradient(135deg, rgba(122, 29, 45, 0.92), rgba(214, 173, 92, 0.72));
  color: #fff7ea;
  white-space: pre-wrap;
}

.assistant-message__body {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.assistant-panel__composer {
  gap: 12px;
  padding-top: 16px;
  padding-bottom: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.assistant-panel__composer textarea {
  width: 100%;
  resize: none;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--theme-accent-text-strong);
  padding: 14px 16px;
  font: inherit;
}

.assistant-panel__composer textarea:focus {
  outline: none;
  border-color: rgba(214, 173, 92, 0.36);
}

.assistant-panel__composer-actions {
  align-items: center;
  justify-content: space-between;
}

.assistant-panel__composer button {
  border: 0;
  background: linear-gradient(135deg, rgba(122, 29, 45, 0.92), rgba(214, 173, 92, 0.84));
  color: #fff7ea;
  padding: 9px 16px;
}

.assistant-panel__composer button:disabled {
  opacity: 0.6;
}

.assistant-markdown :deep(h1),
.assistant-markdown :deep(h2),
.assistant-markdown :deep(h3),
.assistant-markdown :deep(h4),
.assistant-markdown :deep(h5),
.assistant-markdown :deep(h6) {
  margin: 1em 0 0.5em;
  line-height: 1.3;
}

.assistant-markdown :deep(p),
.assistant-markdown :deep(blockquote),
.assistant-markdown :deep(ul),
.assistant-markdown :deep(ol),
.assistant-markdown :deep(table) {
  margin: 0 0 14px;
}

.assistant-markdown :deep(blockquote) {
  margin-left: 0;
  padding: 10px 14px;
  border-left: 3px solid var(--theme-accent-text);
  background: rgba(255, 247, 234, 0.05);
  border-radius: 12px;
}

.assistant-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.assistant-markdown :deep(th),
.assistant-markdown :deep(td) {
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 8px 10px;
  text-align: left;
}

.assistant-markdown :deep(code) {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
}

:deep(.assistant-markdown__image) {
  width: 100%;
  border-radius: 16px;
  display: block;
}

:deep(.assistant-code) {
  margin: 16px 0;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(0, 0, 0, 0.24);
}

:deep(.assistant-code__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 12px;
}

:deep(.assistant-code__copy) {
  border: 0;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.24);
  color: #fff7ea;
  padding: 6px 10px;
  cursor: pointer;
}

:deep(.assistant-code__body) {
  margin: 0;
  padding: 0 14px 14px;
  overflow-x: auto;
}

:deep(.assistant-code__body code) {
  padding: 0;
  border-radius: 0;
  background: transparent;
}

.assistant-mask {
  position: fixed;
  inset: 0;
  background: rgba(6, 2, 4, 0.48);
  backdrop-filter: blur(6px);
}

.assistant-trigger-enter-active,
.assistant-trigger-leave-active {
  transition: opacity 0.24s ease, transform 0.34s cubic-bezier(0.22, 1, 0.36, 1), filter 0.24s ease;
}

.assistant-trigger-enter-from,
.assistant-trigger-leave-to {
  opacity: 0;
  filter: blur(10px);
  transform: translateY(18px) scale(0.82);
}

.assistant-mask-enter-active,
.assistant-mask-leave-active {
  transition: opacity 0.24s ease;
}

.assistant-mask-enter-from,
.assistant-mask-leave-to {
  opacity: 0;
}

.assistant-panel-enter-active,
.assistant-panel-leave-active {
  transform-origin: bottom right;
  transition:
    opacity 0.32s ease,
    transform 0.42s cubic-bezier(0.18, 0.88, 0.2, 1),
    filter 0.32s ease;
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  filter: blur(14px);
  transform: translateY(26px) translateX(8px) scale(0.88);
}

.assistant-panel-enter-to,
.assistant-panel-leave-from {
  opacity: 1;
  filter: blur(0);
  transform: translateY(0) translateX(0) scale(1);
}

@keyframes assistant-tail {
  0%,
  100% {
    transform: rotate(14deg);
  }

  50% {
    transform: rotate(-8deg);
  }
}

@keyframes assistant-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-6px);
  }
}

@keyframes assistant-bob {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-4px);
  }
}

@keyframes assistant-blink {
  0%,
  44%,
  48%,
  100% {
    transform: scaleY(1);
  }

  46% {
    transform: scaleY(0.15);
  }
}

@media (max-width: 768px) {
  .assistant-shell {
    left: 0;
    right: 0;
    bottom: 16px;
    display: flex;
    justify-content: center;
    pointer-events: none;
  }

  .assistant-trigger,
  .assistant-panel {
    pointer-events: auto;
  }

  .assistant-trigger {
    width: min(320px, calc(100vw - 32px));
    grid-template-columns: 80px minmax(0, 1fr);
    padding: 12px;
    border-radius: 26px;
  }

  .assistant-trigger__bubble {
    min-height: 88px;
    padding: 14px;
  }

  .assistant-trigger__title {
    font-size: 1.35rem;
  }

  .assistant-panel {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 12px;
    width: auto;
    height: min(80vh, 680px);
    border-radius: 24px;
  }

  .assistant-panel__header,
  .assistant-panel__context,
  .assistant-panel__messages,
  .assistant-panel__composer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .assistant-panel__actions {
    flex-direction: column;
  }

  .assistant-panel-enter-active,
  .assistant-panel-leave-active {
    transform-origin: bottom center;
  }

  .assistant-panel-enter-from,
  .assistant-panel-leave-to {
    transform: translateY(24px) scale(0.96);
  }
}
</style>
