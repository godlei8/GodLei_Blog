<template>
  <div
    v-if="assistantEnabled"
    class="assistant-shell"
    :class="{
      'is-open': open,
      'is-mobile': isMobile,
      'is-ready': positionReady,
      'is-dock-left': dockSide === 'left',
      'is-dock-right': dockSide === 'right',
      'is-dragging': dragging,
      'is-snap-left': snapPreviewSide === 'left',
      'is-snap-right': snapPreviewSide === 'right',
    }"
    :style="shellStyle"
  >
    <div v-if="dragging && !isMobile" class="assistant-edge assistant-edge--left" :class="{ 'is-active': snapPreviewSide === 'left' }"></div>
    <div v-if="dragging && !isMobile" class="assistant-edge assistant-edge--right" :class="{ 'is-active': snapPreviewSide === 'right' }"></div>

    <transition name="assistant-intro">
      <div v-if="shouldShowIdleIntro" class="assistant-intro" :class="`is-${dockSide}`" aria-hidden="true">
        <strong>{{ assistantName }}</strong>
        <p>{{ idleIntroText }}</p>
        <small class="assistant-intro__meta">拖动可贴边，轻点即可开始 AI 对话</small>
      </div>
    </transition>

    <transition name="assistant-trigger">
      <button
        v-if="!open"
        class="assistant-trigger"
        type="button"
        :aria-label="`打开 ${assistantName}`"
        @click="handleTriggerClick"
        @pointerdown="handleTriggerPointerDown"
        @pointermove="handleTriggerPointerMove"
        @pointerup="handleTriggerPointerUp"
        @pointercancel="handleTriggerPointerCancel"
        @lostpointercapture="handleTriggerLostPointerCapture"
      >
        <div class="assistant-trigger__orb" aria-hidden="true">
          <span class="assistant-trigger__glow assistant-trigger__glow--outer"></span>
          <span class="assistant-trigger__glow assistant-trigger__glow--inner"></span>
          <span class="assistant-trigger__ring assistant-trigger__ring--one"></span>
          <span class="assistant-trigger__ring assistant-trigger__ring--two"></span>
          <span class="assistant-trigger__core"></span>
          <span class="assistant-trigger__symbol">
            <svg viewBox="0 0 40 40" role="presentation" focusable="false">
              <path
                class="assistant-symbol__cat"
                d="M12.4 14.8L16.2 10.9C16.9 10.2 18.1 10.6 18.1 11.6V13.3C19.3 12.9 20.7 12.9 21.9 13.3V11.6C21.9 10.6 23.1 10.2 23.8 10.9L27.6 14.8C29.3 16.5 30.2 18.8 30.2 21.2C30.2 26.6 25.8 31 20.4 31H19.6C14.2 31 9.8 26.6 9.8 21.2C9.8 18.8 10.7 16.5 12.4 14.8Z"
              />
              <circle class="assistant-symbol__eye" cx="16.9" cy="21" r="1.45" />
              <circle class="assistant-symbol__eye" cx="23.1" cy="21" r="1.45" />
              <path class="assistant-symbol__smile" d="M16.4 24.2C17.5 25.9 19 26.8 20 26.8C21 26.8 22.5 25.9 23.6 24.2" />
            </svg>
          </span>
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
              <span class="assistant-panel__avatar-glow"></span>
              <span class="assistant-panel__avatar-ring"></span>
              <span class="assistant-panel__avatar-core"></span>
              <span class="assistant-panel__avatar-symbol">
                <svg viewBox="0 0 40 40" role="presentation" focusable="false">
                  <path
                    class="assistant-symbol__cat"
                    d="M12.4 14.8L16.2 10.9C16.9 10.2 18.1 10.6 18.1 11.6V13.3C19.3 12.9 20.7 12.9 21.9 13.3V11.6C21.9 10.6 23.1 10.2 23.8 10.9L27.6 14.8C29.3 16.5 30.2 18.8 30.2 21.2C30.2 26.6 25.8 31 20.4 31H19.6C14.2 31 9.8 26.6 9.8 21.2C9.8 18.8 10.7 16.5 12.4 14.8Z"
                  />
                  <circle class="assistant-symbol__eye" cx="16.9" cy="21" r="1.45" />
                  <circle class="assistant-symbol__eye" cx="23.1" cy="21" r="1.45" />
                  <path class="assistant-symbol__smile" d="M16.4 24.2C17.5 25.9 19 26.8 20 26.8C21 26.8 22.5 25.9 23.6 24.2" />
                </svg>
              </span>
            </div>

            <div class="assistant-panel__copy">
              <span class="assistant-panel__eyebrow">AI Assistant</span>
              <h2>{{ assistantName }}</h2>
              <p>{{ assistantSubtitle }}</p>
            </div>
          </div>

          <div class="assistant-panel__actions">
            <button type="button" @click="clearConversation">清空</button>
            <button type="button" @click="closePanel">关闭</button>
          </div>
        </header>

        <div ref="messagesRef" class="assistant-panel__messages" @wheel.prevent="handleMessagesWheel">
          <div
            v-if="!messages.length"
            class="assistant-empty"
            :class="{
              'is-single': emptyStateSectionCount === 1,
              'is-double': emptyStateSectionCount === 2,
            }"
          >
            <div v-if="hasWelcomeContent || hasDisclaimerContent" class="assistant-empty__intro">
              <div
                v-if="hasWelcomeContent"
                class="assistant-empty__welcome assistant-empty__card assistant-markdown"
                v-html="welcomeHtml"
              ></div>
              <div
                v-if="hasDisclaimerContent"
                class="assistant-empty__disclaimer assistant-empty__card assistant-markdown"
                v-html="disclaimerHtml"
              ></div>
            </div>

            <div v-if="hasStarterPrompts" class="assistant-empty__starter-group assistant-empty__card">
              <span class="assistant-empty__label">可以这样问我</span>

              <div class="assistant-empty__starters">
                <button
                  v-for="prompt in starterPrompts"
                  :key="prompt"
                  type="button"
                  @click="submitMessage(prompt)"
                >
                  {{ prompt }}
                </button>
              </div>
            </div>

            <div v-if="!hasEmptyStateContent" class="assistant-empty__fallback assistant-empty__card">
              <strong>{{ assistantName }}</strong>
              <p>直接向我提问，我会结合当前页面内容帮你梳理重点。</p>
            </div>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="assistant-message"
            :class="[
              `is-${message.role}`,
              {
                'is-pending': message.pending,
              },
            ]"
          >
            <div class="assistant-message__meta">
              <strong>{{ message.role === 'user' ? '我' : assistantName }}</strong>
              <span v-if="message.pending" class="assistant-message__meta-status">
                <span class="assistant-message__meta-status-icon" aria-hidden="true">
                  <svg viewBox="0 0 18 18" role="presentation" focusable="false">
                    <path d="M9 3.2C6.13 3.2 3.8 5.53 3.8 8.4C3.8 10.17 4.68 11.74 6.04 12.69C6.51 13.01 6.78 13.56 6.78 14.13V14.4C6.78 14.84 7.14 15.2 7.58 15.2H10.42C10.86 15.2 11.22 14.84 11.22 14.4V14.13C11.22 13.56 11.49 13.01 11.96 12.69C13.32 11.74 14.2 10.17 14.2 8.4C14.2 5.53 11.87 3.2 9 3.2Z" />
                    <path d="M7.22 15.2H10.78C10.78 16.07 10.08 16.78 9.2 16.78H8.8C7.92 16.78 7.22 16.07 7.22 15.2Z" />
                    <path d="M6.65 2.32L5.92 1.48" />
                    <path d="M11.35 2.32L12.08 1.48" />
                    <path d="M4.4 4.55L3.36 4.08" />
                    <path d="M13.6 4.55L14.64 4.08" />
                  </svg>
                </span>
                <span>思考中</span>
              </span>
            </div>

            <div
              v-if="message.role === 'assistant' && message.pending && !message.content"
              class="assistant-message__body assistant-message__body--pending"
            >
              <span class="assistant-message__typing" aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
              </span>
              <div class="assistant-message__pending-copy" aria-hidden="true">
                <span class="assistant-message__pending-line assistant-message__pending-line--long" aria-hidden="true"></span>
                <span class="assistant-message__pending-line assistant-message__pending-line--short" aria-hidden="true"></span>
              </div>
            </div>

            <div
              v-else-if="message.role === 'assistant'"
              class="assistant-message__assistant-stack"
            >
              <div
                class="assistant-message__body assistant-markdown"
                :class="{ 'assistant-message__body--streaming': message.pending }"
                v-html="message.html"
              ></div>
              <div v-if="message.pending" class="assistant-message__streaming-indicator" aria-hidden="true">
                <span class="assistant-message__streaming-pulse" aria-hidden="true"></span>
              </div>
            </div>
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
import { resolveApiUrl } from '@/utils/apiBase'
import { pageContextState } from '@/utils/pageContext'
import { getDefaultSiteConfig, loadSiteConfig } from '@/utils/siteConfig'

const STORAGE_KEY = 'godlei-assistant-session'
const FLOATING_KEY = 'godlei-assistant-floating'
const DESKTOP_TRIGGER_SIZE = 72
const MOBILE_TRIGGER_SIZE = 62
const EMPTY_REPLY_FALLBACK = '刚刚这条回复没有成功生成，你可以再问我一次。'
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
const showIdleIntro = ref(false)
const dockSide = ref('right')
const snapPreviewSide = ref('')
const snapProximity = ref(0)
const positionReady = ref(false)
const triggerPosition = reactive({
  x: 0,
  y: 0,
})
const dragState = reactive({
  active: false,
  pointerId: null,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0,
  moved: false,
  suppressClick: false,
})

let idleIntroShowTimer = 0
let idleIntroHideTimer = 0
let suppressClickTimer = 0

const assistantEnabled = computed(() => siteConfig.value.assistant?.enabled !== false)
const assistantName = computed(() => siteConfig.value.assistant?.name || '馨宝')
const assistantSubtitle = computed(() => siteConfig.value.assistant?.subtitle || '站内 AI 助手')
const starterPrompts = computed(() => siteConfig.value.assistant?.starterPrompts || [])
const welcomeContent = computed(() => String(siteConfig.value.assistant?.welcomeMessage ?? '').trim())
const disclaimerContent = computed(() => String(siteConfig.value.assistant?.disclaimer ?? '').trim())
const hasStarterPrompts = computed(() => starterPrompts.value.length > 0)
const hasWelcomeContent = computed(() => Boolean(welcomeContent.value))
const hasDisclaimerContent = computed(() => Boolean(disclaimerContent.value))
const hasEmptyStateContent = computed(() => hasWelcomeContent.value || hasDisclaimerContent.value || hasStarterPrompts.value)
const emptyStateSectionCount = computed(() => {
  return Number(hasWelcomeContent.value) + Number(hasDisclaimerContent.value) + Number(hasStarterPrompts.value)
})
const welcomeHtml = computed(() => renderMarkdown(welcomeContent.value))
const disclaimerHtml = computed(() => renderMarkdown(disclaimerContent.value))
const dragging = computed(() => dragState.active)
const shouldShowIdleIntro = computed(() => showIdleIntro.value && !open.value && !isMobile.value)
const idleIntroText = computed(() => `我是${assistantName.value}，会帮你找内容、答问题，也能陪你理理思路。`)
const shellStyle = computed(() => {
  const style = {
    '--assistant-snap-strength': snapProximity.value.toFixed(3),
  }

  if (!positionReady.value) {
    const inset = getViewportInset()
    return {
      ...style,
      right: `${inset}px`,
      bottom: `${inset}px`,
      left: 'auto',
      top: 'auto',
    }
  }

  return {
    ...style,
    left: `${triggerPosition.x}px`,
    top: `${triggerPosition.y}px`,
    right: 'auto',
    bottom: 'auto',
  }
})

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

function ensureAssistantMessageContent(message, fallback = EMPTY_REPLY_FALLBACK) {
  if (!message) return false
  if (String(message.content || '').trim()) return false
  message.content = fallback
  return true
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

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function getTriggerSize() {
  return isMobile.value ? MOBILE_TRIGGER_SIZE : DESKTOP_TRIGGER_SIZE
}

function getViewportWidth() {
  if (typeof window === 'undefined') return 0
  return document.documentElement?.clientWidth || window.innerWidth
}

function getViewportHeight() {
  if (typeof window === 'undefined') return 0
  return document.documentElement?.clientHeight || window.innerHeight
}

function getViewportInset() {
  return isMobile.value ? 10 : 8
}

function getEdgeSnapThreshold() {
  return isMobile.value ? 26 : 72
}

function normalizeTriggerPosition(x, y) {
  if (typeof window === 'undefined') {
    return { x, y }
  }

  const inset = getViewportInset()
  const triggerSize = getTriggerSize()
  const maxX = Math.max(inset, getViewportWidth() - triggerSize - inset)
  const maxY = Math.max(inset, getViewportHeight() - triggerSize - inset)

  return {
    x: clamp(x, inset, maxX),
    y: clamp(y, inset, maxY),
  }
}

function resolveDockSide(x) {
  if (typeof window === 'undefined') return 'right'
  return x + getTriggerSize() / 2 <= getViewportWidth() / 2 ? 'left' : 'right'
}

function getSnapMetrics(x) {
  if (typeof window === 'undefined') {
    return {
      side: '',
      proximity: 0,
    }
  }

  const inset = getViewportInset()
  const triggerSize = getTriggerSize()
  const maxX = Math.max(inset, getViewportWidth() - triggerSize - inset)
  const threshold = getEdgeSnapThreshold()
  const distanceLeft = Math.abs(x - inset)
  const distanceRight = Math.abs(maxX - x)

  if (distanceLeft > threshold && distanceRight > threshold) {
    return {
      side: '',
      proximity: 0,
    }
  }

  const side = distanceLeft <= distanceRight ? 'left' : 'right'
  const distance = side === 'left' ? distanceLeft : distanceRight

  return {
    side,
    proximity: clamp(1 - distance / threshold, 0, 1),
  }
}

function getMagneticPosition(x, side, proximity) {
  if (typeof window === 'undefined' || !side || proximity <= 0) {
    return x
  }

  const inset = getViewportInset()
  const triggerSize = getTriggerSize()
  const maxX = Math.max(inset, getViewportWidth() - triggerSize - inset)
  const targetX = side === 'left' ? inset : maxX
  const pullRatio = 0.16 + proximity * 0.14

  return clamp(x + (targetX - x) * pullRatio, inset, maxX)
}

function getDefaultTriggerPosition() {
  if (typeof window === 'undefined') {
    return { x: 0, y: 0 }
  }

  const inset = getViewportInset()
  const triggerSize = getTriggerSize()
  return normalizeTriggerPosition(getViewportWidth() - triggerSize - inset, getViewportHeight() - triggerSize - inset)
}

function persistTriggerPosition() {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(
    FLOATING_KEY,
    JSON.stringify({
      x: triggerPosition.x,
      y: triggerPosition.y,
      dockSide: dockSide.value,
    })
  )
}

function snapTriggerToEdge(shouldPersist = true) {
  if (typeof window === 'undefined') return

  const inset = getViewportInset()
  const triggerSize = getTriggerSize()
  const maxX = Math.max(inset, getViewportWidth() - triggerSize - inset)
  const nextX = dockSide.value === 'left' ? inset : maxX
  const nextPosition = normalizeTriggerPosition(nextX, triggerPosition.y)

  triggerPosition.x = nextPosition.x
  triggerPosition.y = nextPosition.y

  if (shouldPersist) {
    persistTriggerPosition()
  }
}

function restoreTriggerPosition() {
  if (typeof window === 'undefined') return

  const fallback = getDefaultTriggerPosition()
  const raw = window.localStorage.getItem(FLOATING_KEY)

  if (!raw) {
    triggerPosition.x = fallback.x
    triggerPosition.y = fallback.y
    dockSide.value = 'right'
    positionReady.value = true
    snapTriggerToEdge(false)
    return
  }

  try {
    const payload = JSON.parse(raw)
    const nextPosition = normalizeTriggerPosition(
      Number.isFinite(payload?.x) ? payload.x : fallback.x,
      Number.isFinite(payload?.y) ? payload.y : fallback.y
    )

    triggerPosition.x = nextPosition.x
    triggerPosition.y = nextPosition.y
    dockSide.value = payload?.dockSide === 'left' ? 'left' : resolveDockSide(nextPosition.x)
  } catch (error) {
    triggerPosition.x = fallback.x
    triggerPosition.y = fallback.y
    dockSide.value = 'right'
  }

  positionReady.value = true
  snapTriggerToEdge(false)
}

function syncTriggerPositionToViewport() {
  if (!positionReady.value) return

  const nextPosition = normalizeTriggerPosition(triggerPosition.x, triggerPosition.y)
  triggerPosition.x = nextPosition.x
  triggerPosition.y = nextPosition.y
  snapTriggerToEdge(false)
}

function updateViewportState() {
  isMobile.value = window.innerWidth <= 768
  if (positionReady.value) {
    syncTriggerPositionToViewport()
  }
}

function clearIdleIntroTimers() {
  if (idleIntroShowTimer) {
    window.clearTimeout(idleIntroShowTimer)
    idleIntroShowTimer = 0
  }

  if (idleIntroHideTimer) {
    window.clearTimeout(idleIntroHideTimer)
    idleIntroHideTimer = 0
  }
}

function hideIdleIntro() {
  clearIdleIntroTimers()
  showIdleIntro.value = false
}

function scheduleIdleIntro(delay = 1800) {
  if (typeof window === 'undefined') return

  clearIdleIntroTimers()
  if (!assistantEnabled.value || open.value || dragState.active || isMobile.value) return

  idleIntroShowTimer = window.setTimeout(() => {
    if (open.value || dragState.active || isMobile.value) return

    showIdleIntro.value = true
    idleIntroHideTimer = window.setTimeout(() => {
      showIdleIntro.value = false
      scheduleIdleIntro(18000)
    }, 3600)
  }, delay)
}

function suppressTriggerClick() {
  if (suppressClickTimer) {
    window.clearTimeout(suppressClickTimer)
  }

  dragState.suppressClick = true
  suppressClickTimer = window.setTimeout(() => {
    dragState.suppressClick = false
  }, 220)
}

function finalizeDrag() {
  if (!dragState.active) return

  const wasDragged = dragState.moved
  dragState.active = false
  dragState.pointerId = null

  if (wasDragged) {
    dockSide.value = snapPreviewSide.value || resolveDockSide(triggerPosition.x)
    snapTriggerToEdge()
    suppressTriggerClick()
  }

  snapPreviewSide.value = ''
  snapProximity.value = 0
  dragState.moved = false
  scheduleIdleIntro(wasDragged ? 3200 : 2200)
}

function handleTriggerPointerDown(event) {
  if (event.pointerType === 'mouse' && event.button !== 0) return

  hideIdleIntro()

  dragState.active = true
  dragState.pointerId = event.pointerId
  dragState.startX = event.clientX
  dragState.startY = event.clientY
  dragState.originX = triggerPosition.x
  dragState.originY = triggerPosition.y
  dragState.moved = false
  snapPreviewSide.value = ''
  snapProximity.value = 0

  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function handleTriggerPointerMove(event) {
  if (!dragState.active || dragState.pointerId !== event.pointerId) return

  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY

  if (!dragState.moved && Math.hypot(deltaX, deltaY) > 6) {
    dragState.moved = true
  }

  const nextPosition = normalizeTriggerPosition(dragState.originX + deltaX, dragState.originY + deltaY)
  const snapMetrics = getSnapMetrics(nextPosition.x)
  const magneticX = getMagneticPosition(nextPosition.x, snapMetrics.side, snapMetrics.proximity)
  triggerPosition.y = nextPosition.y
  triggerPosition.x = magneticX
  snapPreviewSide.value = snapMetrics.side
  snapProximity.value = snapMetrics.proximity
  dockSide.value = snapMetrics.side || resolveDockSide(magneticX)
}

function handleTriggerPointerUp(event) {
  if (dragState.pointerId !== event.pointerId) return
  event.currentTarget.releasePointerCapture?.(event.pointerId)
  finalizeDrag()
}

function handleTriggerPointerCancel(event) {
  if (dragState.pointerId !== event.pointerId) return
  finalizeDrag()
}

function handleTriggerLostPointerCapture() {
  finalizeDrag()
}

function handleTriggerClick(event) {
  if (dragState.suppressClick) {
    event.preventDefault()
    event.stopPropagation()
    return
  }

  openPanel()
}

function openPanel() {
  hideIdleIntro()
  open.value = true
  nextTick(scrollMessagesToBottom)
}

function closePanel() {
  open.value = false
  scheduleIdleIntro(1800)
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
    const response = await fetch(resolveApiUrl('/assistant/chat/stream'), {
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
        ensureAssistantMessageContent(assistantMessage)
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
    ensureAssistantMessageContent(assistantMessage)
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

watch(open, (value) => {
  if (value) {
    hideIdleIntro()
  } else {
    scheduleIdleIntro(1800)
  }
})

watch(assistantEnabled, (value) => {
  if (!value) {
    hideIdleIntro()
    open.value = false
    return
  }

  scheduleIdleIntro(1800)
})

watch(isMobile, (value) => {
  if (value) {
    hideIdleIntro()
    return
  }

  scheduleIdleIntro(2400)
})

onMounted(async () => {
  restoreMessages()
  restoreTriggerPosition()
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
  scheduleIdleIntro(2200)
})

onBeforeUnmount(() => {
  if (requestController.value) {
    requestController.value.abort()
  }
  renderTimers.forEach((timer) => window.clearTimeout(timer))
  clearIdleIntroTimers()
  if (suppressClickTimer) {
    window.clearTimeout(suppressClickTimer)
  }
  window.removeEventListener('resize', updateViewportState)
  window.removeEventListener('storage', handleStorage)
  window.removeEventListener('focus', handleFocus)
})
</script>

<style scoped>
.assistant-shell {
  position: fixed;
  z-index: 1300;
  pointer-events: none;
}

.assistant-shell.is-ready {
  transition:
    left 0.34s cubic-bezier(0.22, 1, 0.24, 1),
    top 0.34s cubic-bezier(0.22, 1, 0.24, 1),
    filter 0.24s ease;
  will-change: left, top;
}

.assistant-trigger,
.assistant-panel {
  pointer-events: auto;
}

.assistant-shell.is-dragging {
  user-select: none;
  transition: none;
}

.assistant-edge {
  position: fixed;
  top: 18px;
  bottom: 18px;
  width: 10px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(214, 173, 92, 0.08), rgba(122, 29, 45, 0.18));
  box-shadow:
    0 0 0 1px rgba(214, 173, 92, 0.12),
    0 0 20px rgba(122, 29, 45, 0.12);
  opacity: 0.24;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.assistant-edge--left {
  left: 6px;
}

.assistant-edge--right {
  right: 6px;
}

.assistant-edge.is-active {
  opacity: calc(0.52 + var(--assistant-snap-strength, 0) * 0.4);
  transform: scaleY(calc(1.01 + var(--assistant-snap-strength, 0) * 0.07));
  box-shadow:
    0 0 0 1px rgba(214, 173, 92, 0.28),
    0 0 26px rgba(214, 173, 92, 0.2);
}

.assistant-intro {
  position: absolute;
  top: 50%;
  width: min(196px, calc(100vw - 90px));
  display: grid;
  gap: 5px;
  padding: 11px 12px;
  border-radius: 16px;
  border: 1px solid rgba(214, 173, 92, 0.14);
  background:
    linear-gradient(180deg, rgba(255, 251, 243, 0.96), rgba(246, 233, 208, 0.92));
  box-shadow:
    0 16px 26px rgba(79, 49, 22, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  color: #3a2417;
  pointer-events: none;
  transform: translateY(-50%);
  backdrop-filter: blur(10px);
}

.assistant-intro::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 12px;
  height: 12px;
  background: inherit;
  border-right: 1px solid rgba(214, 173, 92, 0.14);
  border-bottom: 1px solid rgba(214, 173, 92, 0.14);
}

.assistant-shell.is-dock-right .assistant-intro {
  right: calc(100% - 2px);
}

.assistant-shell.is-dock-right .assistant-intro::after {
  right: -6px;
  transform: translateY(-50%) rotate(-45deg);
}

.assistant-shell.is-dock-left .assistant-intro {
  left: calc(100% - 2px);
}

.assistant-shell.is-dock-left .assistant-intro::after {
  left: -6px;
  transform: translateY(-50%) rotate(135deg);
}

.assistant-intro strong {
  font-size: 15px;
  line-height: 1.15;
  color: #56331e;
}

.assistant-intro p {
  margin: 0;
  color: rgba(77, 51, 29, 0.74);
  font-size: 11px;
  line-height: 1.55;
}

.assistant-intro__meta {
  color: rgba(139, 96, 44, 0.8);
  font-size: 10px;
  line-height: 1.45;
}

.assistant-trigger {
  width: 72px;
  height: 72px;
  padding: 0;
  border: 0;
  background: transparent;
  display: grid;
  place-items: center;
  cursor: pointer;
  touch-action: none;
  transition: transform 0.3s ease, filter 0.3s ease;
}

.assistant-trigger:hover {
  transform: translateY(-5px) scale(1.03);
  filter: drop-shadow(0 18px 24px rgba(31, 12, 8, 0.28));
}

.assistant-trigger:active {
  transform: translateY(-2px) scale(0.98);
}

.assistant-shell.is-dragging .assistant-trigger {
  cursor: grabbing;
  filter: drop-shadow(0 16px 20px rgba(87, 53, 21, 0.16));
  transition: none;
}

.assistant-shell.is-dragging .assistant-trigger__orb {
  animation-duration: 2.6s;
}

.assistant-shell.is-dragging.is-dock-left .assistant-trigger {
  transform: translateX(calc(-2px - var(--assistant-snap-strength, 0) * 6px))
    scale(calc(0.92 + var(--assistant-snap-strength, 0) * 0.05));
}

.assistant-shell.is-dragging.is-dock-right .assistant-trigger {
  transform: translateX(calc(2px + var(--assistant-snap-strength, 0) * 6px))
    scale(calc(0.92 + var(--assistant-snap-strength, 0) * 0.05));
}

.assistant-shell.is-snap-left .assistant-trigger,
.assistant-shell.is-snap-right .assistant-trigger {
  filter: drop-shadow(0 0 calc(16px + var(--assistant-snap-strength, 0) * 14px) rgba(214, 173, 92, 0.22));
}

.assistant-trigger__orb {
  position: relative;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  animation: assistant-orb-float 3.8s ease-in-out infinite;
}

.assistant-trigger__orb::before,
.assistant-trigger__orb::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
}

.assistant-trigger__orb::before {
  background:
    radial-gradient(circle at 42% 24%, rgba(255, 227, 170, 0.52), transparent 34%),
    radial-gradient(circle at 70% 74%, rgba(141, 27, 46, 0.58), transparent 56%),
    radial-gradient(circle at 50% 50%, rgba(10, 3, 6, 0.96), rgba(20, 5, 10, 0.86) 68%, rgba(124, 38, 56, 0.44));
  box-shadow:
    inset 0 -14px 28px rgba(8, 2, 4, 0.66),
    inset 0 0 38px rgba(214, 173, 92, 0.2),
    0 0 18px rgba(214, 173, 92, 0.18);
}

.assistant-trigger__orb::after {
  inset: -10px;
  background: radial-gradient(circle, rgba(214, 173, 92, 0.24), rgba(122, 29, 45, 0.02) 66%, transparent 78%);
  filter: blur(5px);
  animation: assistant-orb-breathe 2.8s ease-in-out infinite;
}

.assistant-trigger__glow,
.assistant-trigger__ring,
.assistant-trigger__core {
  position: absolute;
  inset: 0;
  border-radius: 50%;
}

.assistant-trigger__glow--outer {
  inset: -12px;
  background: radial-gradient(circle, rgba(122, 29, 45, 0.16), rgba(214, 173, 92, 0.04) 58%, transparent 74%);
  filter: blur(8px);
  animation: assistant-orb-breathe 3.4s ease-in-out infinite;
}

.assistant-trigger__glow--inner {
  inset: 10px;
  background: radial-gradient(circle, rgba(255, 247, 227, 0.28), rgba(214, 173, 92, 0) 62%);
  mix-blend-mode: screen;
  animation: assistant-orb-pulse 2.6s ease-in-out infinite;
}

.assistant-trigger__ring {
  border: 2px solid transparent;
  mix-blend-mode: screen;
  opacity: 0.72;
}

.assistant-trigger__ring--one {
  inset: 4px;
  border-top-color: rgba(214, 173, 92, 0.86);
  border-left-color: rgba(214, 173, 92, 0.42);
  border-right-color: rgba(122, 29, 45, 0.42);
  animation: assistant-orb-spin 7.2s linear infinite;
}

.assistant-trigger__ring--two {
  inset: 8px;
  border-bottom-color: rgba(214, 173, 92, 0.72);
  border-right-color: rgba(214, 173, 92, 0.36);
  border-left-color: rgba(122, 29, 45, 0.32);
  animation: assistant-orb-spin-reverse 5.4s linear infinite;
}

.assistant-trigger__core {
  inset: 14px;
  background:
    radial-gradient(circle at 30% 28%, rgba(255, 243, 219, 0.4), transparent 42%),
    radial-gradient(circle at 72% 70%, rgba(122, 29, 45, 0.52), transparent 64%),
    radial-gradient(circle at 50% 50%, rgba(32, 8, 16, 0.84), rgba(16, 4, 9, 0.96));
  filter: blur(0.4px);
  animation: assistant-orb-sway 4.6s ease-in-out infinite;
}

.assistant-trigger__symbol,
.assistant-panel__avatar-symbol {
  position: absolute;
  z-index: 4;
  display: grid;
  place-items: center;
  filter: drop-shadow(0 0 8px rgba(230, 190, 122, 0.18));
  opacity: 0.98;
}

.assistant-trigger__symbol {
  inset: 13px;
  animation: assistant-symbol-flicker 3.8s ease-in-out infinite;
}

.assistant-panel__avatar-symbol {
  inset: 9px;
}

.assistant-trigger__symbol svg,
.assistant-panel__avatar-symbol svg {
  width: 100%;
  height: 100%;
}

.assistant-symbol__cat {
  fill: rgba(235, 206, 152, 0.92);
  stroke: rgba(245, 223, 181, 0.84);
  stroke-width: 1.15;
  stroke-linejoin: round;
}

.assistant-symbol__eye {
  fill: rgba(114, 70, 37, 0.88);
}

.assistant-symbol__smile {
  fill: none;
  stroke: rgba(114, 70, 37, 0.82);
  stroke-width: 1.45;
  stroke-linecap: round;
}

.assistant-panel {
  position: fixed;
  right: 12px;
  bottom: 18px;
  width: min(456px, calc(100% - 20px));
  height: min(82vh, 780px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  border-radius: 30px;
  border: 1px solid var(--theme-accent-border-soft);
  background:
    radial-gradient(circle at 100% 0%, rgba(214, 173, 92, 0.2), transparent 0 28%),
    radial-gradient(circle at 12% 12%, rgba(214, 173, 92, 0.08), transparent 0 18%),
    linear-gradient(180deg, rgba(26, 9, 14, 0.98), rgba(16, 5, 9, 0.985) 56%, rgba(11, 4, 6, 0.99));
  box-shadow:
    0 34px 60px rgba(8, 2, 4, 0.46),
    inset 0 1px 0 rgba(246, 231, 198, 0.08);
  overflow: hidden;
  --assistant-panel-origin: bottom right;
  --assistant-panel-shift-x: 6px;
}

.assistant-shell.is-dock-left .assistant-panel {
  left: 12px;
  right: auto;
  --assistant-panel-origin: bottom left;
  --assistant-panel-shift-x: -6px;
}

.assistant-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 17px 18px 13px;
  border-bottom: 1px solid rgba(214, 173, 92, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 247, 234, 0.02), rgba(255, 247, 234, 0)),
    linear-gradient(90deg, rgba(214, 173, 92, 0.04), rgba(214, 173, 92, 0) 32%);
}

.assistant-panel__brand {
  display: flex;
  align-items: center;
  gap: 11px;
}

.assistant-panel__avatar {
  position: relative;
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border-radius: 50%;
  overflow: hidden;
  background: radial-gradient(circle at 34% 24%, rgba(255, 232, 189, 0.4), rgba(26, 7, 13, 0.94) 72%);
  box-shadow:
    inset 0 0 18px rgba(214, 173, 92, 0.22),
    0 8px 16px rgba(8, 2, 4, 0.28);
}

.assistant-panel__avatar-glow,
.assistant-panel__avatar-ring,
.assistant-panel__avatar-core {
  position: absolute;
  border-radius: 50%;
}

.assistant-panel__avatar-glow {
  inset: -8px;
  background: radial-gradient(circle, rgba(214, 173, 92, 0.24), transparent 72%);
  filter: blur(6px);
}

.assistant-panel__avatar-ring {
  inset: 5px;
  border: 1.5px solid rgba(214, 173, 92, 0.56);
  border-left-color: rgba(122, 29, 45, 0.42);
  border-bottom-color: rgba(122, 29, 45, 0.2);
}

.assistant-panel__avatar-core {
  inset: 14px;
  background: radial-gradient(circle at 28% 24%, rgba(255, 245, 225, 0.42), rgba(122, 29, 45, 0.48) 60%, rgba(16, 4, 9, 0.98));
}

.assistant-panel__copy {
  display: grid;
  gap: 3px;
  padding-top: 1px;
}

.assistant-panel__eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(214, 173, 92, 0.1);
  color: var(--theme-accent-text);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.assistant-panel__header h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.1;
  color: var(--theme-accent-text-strong);
}

.assistant-panel__header p,
.assistant-message__meta,
.assistant-panel__error {
  margin: 0;
  color: rgba(246, 231, 198, 0.72);
  line-height: 1.45;
}

.assistant-panel__actions,
.assistant-empty__starters,
.assistant-panel__composer-actions {
  display: flex;
  gap: 10px;
}

.assistant-panel__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.assistant-panel__actions button,
.assistant-panel__composer button,
.assistant-empty__starters button {
  border-radius: 999px;
  cursor: pointer;
  transition:
    transform 0.22s ease,
    background-color 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.assistant-panel__actions button,
.assistant-empty__starters button {
  border: 1px solid rgba(214, 173, 92, 0.16);
  background: linear-gradient(135deg, rgba(74, 18, 29, 0.34), rgba(18, 7, 11, 0.9));
  color: var(--theme-accent-text-soft);
  padding: 8px 13px;
  box-shadow: inset 0 1px 0 rgba(246, 231, 198, 0.05);
}

.assistant-panel__messages,
.assistant-panel__composer {
  padding-left: 18px;
  padding-right: 18px;
}

.assistant-panel__messages {
  overflow-y: auto;
  padding-top: 16px;
  padding-bottom: 16px;
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(246, 231, 198, 0.04), rgba(246, 231, 198, 0.015) 12%, rgba(214, 173, 92, 0.04) 100%);
  scrollbar-width: thin;
  scrollbar-color: rgba(214, 173, 92, 0.32) rgba(255, 247, 234, 0.04);
}

.assistant-empty,
.assistant-message,
.assistant-panel__composer {
  display: grid;
}

.assistant-empty {
  gap: 12px;
  align-content: start;
  justify-items: start;
  padding-bottom: 4px;
}

.assistant-empty__intro,
.assistant-empty__starter-group,
.assistant-empty__fallback {
  display: grid;
  gap: 10px;
  justify-items: start;
}

.assistant-empty__card {
  width: fit-content;
  max-width: 100%;
}

.assistant-empty.is-single {
  gap: 10px;
}

.assistant-empty.is-double .assistant-empty__intro {
  gap: 8px;
}

.assistant-empty__starter-group {
  padding: 13px 14px;
  border-radius: 18px;
  border: 1px solid rgba(214, 173, 92, 0.12);
  background:
    linear-gradient(180deg, rgba(246, 231, 198, 0.04), rgba(246, 231, 198, 0.015)),
    rgba(13, 5, 8, 0.56);
  box-shadow: inset 0 1px 0 rgba(246, 231, 198, 0.03);
}

.assistant-empty__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--theme-accent-text);
}

.assistant-message {
  gap: 10px;
  animation: assistant-message-rise 0.3s cubic-bezier(0.18, 0.88, 0.22, 1) both;
}

.assistant-message.is-user {
  justify-items: end;
}

.assistant-message__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 6px;
  font-size: 12px;
  letter-spacing: 0.02em;
}

.assistant-message.is-user .assistant-message__meta {
  justify-content: flex-end;
}

.assistant-message__meta-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(214, 173, 92, 0.74);
  letter-spacing: 0.02em;
}

.assistant-message__meta-status-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: rgba(231, 199, 119, 0.92);
  filter: drop-shadow(0 0 10px rgba(214, 173, 92, 0.14));
  animation: assistant-thinking-glow 1.8s ease-in-out infinite;
}

.assistant-message__meta-status-icon svg {
  width: 100%;
  height: 100%;
}

.assistant-message__meta-status-icon svg path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.assistant-message__assistant-stack {
  display: grid;
  justify-items: start;
  gap: 8px;
  justify-self: start;
  align-self: start;
  width: auto;
  max-width: min(88%, 360px);
}

.assistant-message__plain,
.assistant-message__body {
  justify-self: start;
  align-self: start;
  width: auto;
  min-width: 0;
  max-width: min(88%, 360px);
  margin: 0;
  padding: 12px 15px;
  border-radius: 22px;
  line-height: 1.68;
  box-shadow: 0 16px 28px rgba(8, 2, 4, 0.18);
}

.assistant-message__plain {
  background: linear-gradient(135deg, rgba(122, 29, 45, 0.92), rgba(214, 173, 92, 0.74));
  border: 1px solid rgba(214, 173, 92, 0.14);
  color: var(--theme-accent-text-strong);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.assistant-message.is-user .assistant-message__plain {
  justify-self: end;
}

.assistant-message__body {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 247, 234, 0.03), rgba(255, 247, 234, 0.015)),
    rgba(21, 8, 12, 0.72);
  border: 1px solid rgba(214, 173, 92, 0.1);
  color: rgba(255, 247, 234, 0.92);
  transition:
    border-color 0.24s ease,
    background 0.24s ease,
    box-shadow 0.24s ease;
}

.assistant-message__body--pending {
  display: inline-flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
  max-width: 248px;
  padding: 12px 14px;
  background:
    linear-gradient(180deg, rgba(255, 247, 234, 0.04), rgba(255, 247, 234, 0.015)),
    rgba(18, 7, 11, 0.82);
}

.assistant-message__body--pending::after,
.assistant-message__body--streaming::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, transparent 18%, rgba(255, 243, 219, 0.05) 38%, rgba(214, 173, 92, 0.18) 50%, rgba(255, 243, 219, 0.05) 62%, transparent 82%);
  transform: translateX(-120%);
  animation: assistant-stream-shimmer 2.3s ease-in-out infinite;
  pointer-events: none;
}

.assistant-message__body--streaming {
  border-color: rgba(214, 173, 92, 0.16);
  box-shadow:
    0 16px 28px rgba(8, 2, 4, 0.18),
    0 0 0 1px rgba(214, 173, 92, 0.04);
}

.assistant-message__typing {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding-top: 4px;
}

.assistant-message__typing span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: rgba(214, 173, 92, 0.8);
  animation: assistant-typing-bounce 1.1s ease-in-out infinite;
}

.assistant-message__typing span:nth-child(2) {
  animation-delay: 0.14s;
}

.assistant-message__typing span:nth-child(3) {
  animation-delay: 0.28s;
}

.assistant-message__pending-copy {
  display: grid;
  gap: 7px;
  min-width: 84px;
  padding-top: 2px;
}

.assistant-message__pending-line {
  display: block;
  height: 6px;
  border-radius: 999px;
  background:
    linear-gradient(90deg, rgba(255, 247, 234, 0.04), rgba(214, 173, 92, 0.26), rgba(255, 247, 234, 0.04));
  background-size: 220% 100%;
  animation: assistant-skeleton-wave 1.7s ease-in-out infinite;
}

.assistant-message__pending-line--long {
  width: 96px;
}

.assistant-message__pending-line--short {
  width: 64px;
  animation-delay: 0.18s;
}

.assistant-message__streaming-indicator {
  display: inline-flex;
  align-items: center;
  padding-left: 8px;
}

.assistant-message__streaming-pulse {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(214, 173, 92, 0.82);
  box-shadow: 0 0 0 0 rgba(214, 173, 92, 0.4);
  animation: assistant-stream-pulse 1.35s ease-out infinite;
}

.assistant-panel__composer {
  gap: 12px;
  padding-top: 14px;
  padding-bottom: 18px;
  border-top: 1px solid rgba(214, 173, 92, 0.12);
  background:
    linear-gradient(180deg, rgba(246, 231, 198, 0.035), rgba(246, 231, 198, 0.015)),
    rgba(12, 4, 7, 0.82);
}

.assistant-panel__composer textarea {
  width: 100%;
  resize: none;
  min-height: 108px;
  border: 1px solid rgba(214, 173, 92, 0.14);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 247, 234, 0.02), rgba(255, 247, 234, 0)),
    rgba(20, 8, 12, 0.88);
  color: var(--theme-accent-text-strong);
  padding: 14px 15px;
  font: inherit;
  line-height: 1.7;
  box-shadow: inset 0 1px 0 rgba(246, 231, 198, 0.04);
}

.assistant-panel__composer textarea:focus {
  outline: none;
  border-color: rgba(214, 173, 92, 0.34);
  box-shadow:
    inset 0 1px 0 rgba(246, 231, 198, 0.08),
    0 0 0 4px rgba(214, 173, 92, 0.12);
}

.assistant-panel__composer-actions {
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.assistant-panel__composer button {
  border: 0;
  background: linear-gradient(135deg, rgba(122, 29, 45, 0.96), rgba(214, 173, 92, 0.84));
  color: var(--theme-accent-text-strong);
  padding: 10px 18px;
  font-weight: 700;
  box-shadow: 0 14px 24px rgba(12, 3, 5, 0.24);
}

.assistant-panel__composer button:disabled {
  opacity: 0.58;
  box-shadow: none;
}

.assistant-panel__actions button:hover,
.assistant-empty__starters button:hover,
.assistant-panel__composer button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.assistant-empty__welcome,
.assistant-empty__disclaimer,
.assistant-empty__fallback {
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(214, 173, 92, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 247, 234, 0.04), rgba(255, 247, 234, 0.015)),
    rgba(14, 5, 8, 0.62);
  box-shadow:
    inset 0 1px 0 rgba(246, 231, 198, 0.04),
    0 16px 24px rgba(8, 2, 4, 0.16);
  color: rgba(255, 247, 234, 0.88);
}

.assistant-empty__welcome {
  background:
    radial-gradient(circle at 100% 0%, rgba(214, 173, 92, 0.12), transparent 0 32%),
    linear-gradient(180deg, rgba(255, 247, 234, 0.045), rgba(255, 247, 234, 0.015)),
    rgba(14, 5, 8, 0.72);
}

.assistant-empty__disclaimer {
  color: rgba(246, 231, 198, 0.64);
  font-size: 12px;
}

.assistant-empty__fallback {
  gap: 6px;
  color: rgba(255, 247, 234, 0.78);
}

.assistant-empty__fallback strong,
.assistant-empty__fallback p {
  margin: 0;
}

.assistant-empty__starters {
  flex-wrap: wrap;
  gap: 8px;
}

.assistant-panel__messages::-webkit-scrollbar {
  width: 8px;
}

.assistant-panel__messages::-webkit-scrollbar-track {
  background: rgba(255, 247, 234, 0.03);
  border-radius: 999px;
}

.assistant-panel__messages::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(214, 173, 92, 0.34), rgba(122, 29, 45, 0.56));
  border: 2px solid rgba(20, 8, 12, 0.18);
}

.assistant-panel__messages::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(214, 173, 92, 0.46), rgba(122, 29, 45, 0.68));
}

.assistant-markdown :deep(h1),
.assistant-markdown :deep(h2),
.assistant-markdown :deep(h3),
.assistant-markdown :deep(h4),
.assistant-markdown :deep(h5),
.assistant-markdown :deep(h6) {
  margin: 1em 0 0.5em;
  line-height: 1.3;
  color: var(--theme-accent-text-strong);
}

.assistant-markdown :deep(p),
.assistant-markdown :deep(blockquote),
.assistant-markdown :deep(ul),
.assistant-markdown :deep(ol),
.assistant-markdown :deep(table) {
  margin: 0 0 14px;
}

.assistant-markdown :deep(*:last-child) {
  margin-bottom: 0;
}

.assistant-markdown :deep(blockquote) {
  margin-left: 0;
  padding: 10px 14px;
  border-left: 3px solid rgba(214, 173, 92, 0.72);
  background: rgba(255, 247, 234, 0.05);
  border-radius: 12px;
}

.assistant-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  background: rgba(255, 247, 234, 0.03);
  overflow: hidden;
}

.assistant-markdown :deep(th),
.assistant-markdown :deep(td) {
  border: 1px solid rgba(214, 173, 92, 0.14);
  padding: 8px 10px;
  text-align: left;
}

.assistant-markdown :deep(code) {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(255, 247, 234, 0.08);
  color: var(--theme-accent-text-soft);
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
  border: 1px solid rgba(214, 173, 92, 0.12);
  background: rgba(8, 3, 5, 0.9);
}

:deep(.assistant-code__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(255, 247, 234, 0.04);
  font-size: 12px;
  color: rgba(255, 244, 222, 0.78);
}

:deep(.assistant-code__copy) {
  border: 0;
  border-radius: 999px;
  background: rgba(214, 173, 92, 0.14);
  color: var(--theme-accent-text-soft);
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

.assistant-intro-enter-active,
.assistant-intro-leave-active {
  transition: opacity 0.24s ease, transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.assistant-intro-enter-from,
.assistant-intro-leave-to {
  opacity: 0;
}

.assistant-shell.is-dock-right .assistant-intro-enter-from,
.assistant-shell.is-dock-right .assistant-intro-leave-to {
  transform: translate(-14px, -50%) scale(0.92);
}

.assistant-shell.is-dock-left .assistant-intro-enter-from,
.assistant-shell.is-dock-left .assistant-intro-leave-to {
  transform: translate(14px, -50%) scale(0.92);
}

.assistant-trigger-enter-active,
.assistant-trigger-leave-active {
  transition: opacity 0.26s ease, transform 0.42s cubic-bezier(0.2, 1, 0.22, 1), filter 0.3s ease;
}

.assistant-trigger-enter-from,
.assistant-trigger-leave-to {
  opacity: 0;
  filter: blur(12px) saturate(0.82);
  transform: translateY(18px) scale(0.74) rotate(-8deg);
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
  transform-origin: var(--assistant-panel-origin, bottom right);
  transition:
    opacity 0.3s ease,
    transform 0.44s cubic-bezier(0.18, 0.92, 0.18, 1),
    filter 0.32s ease;
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  filter: blur(12px);
  transform: translateY(24px) translateX(var(--assistant-panel-shift-x, 6px)) scale(0.9);
}

.assistant-panel-enter-to,
.assistant-panel-leave-from {
  opacity: 1;
  filter: blur(0);
  transform: translateY(0) translateX(0) scale(1);
}

@keyframes assistant-orb-float {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }

  50% {
    transform: translateY(-7px) scale(1.02);
  }
}

@keyframes assistant-orb-breathe {
  0%,
  100% {
    opacity: 0.7;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.06);
  }
}

@keyframes assistant-message-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes assistant-typing-bounce {
  0%,
  80%,
  100% {
    opacity: 0.38;
    transform: translateY(0) scale(0.92);
  }

  40% {
    opacity: 1;
    transform: translateY(-2px) scale(1);
  }
}

@keyframes assistant-stream-shimmer {
  0% {
    transform: translateX(-120%);
  }

  55%,
  100% {
    transform: translateX(120%);
  }
}

@keyframes assistant-skeleton-wave {
  0%,
  100% {
    background-position: 100% 50%;
    opacity: 0.62;
  }

  50% {
    background-position: 0% 50%;
    opacity: 1;
  }
}

@keyframes assistant-stream-pulse {
  0% {
    transform: scale(0.92);
    box-shadow: 0 0 0 0 rgba(214, 173, 92, 0.38);
  }

  60% {
    transform: scale(1);
    box-shadow: 0 0 0 8px rgba(214, 173, 92, 0);
  }

  100% {
    transform: scale(0.92);
    box-shadow: 0 0 0 0 rgba(214, 173, 92, 0);
  }
}

@keyframes assistant-thinking-glow {
  0%,
  100% {
    opacity: 0.72;
    transform: translateY(0) scale(0.96);
  }

  50% {
    opacity: 1;
    transform: translateY(-1px) scale(1);
  }
}

@keyframes assistant-orb-pulse {
  0%,
  100% {
    opacity: 0.52;
    transform: scale(1);
  }

  50% {
    opacity: 0.84;
    transform: scale(1.12);
  }
}

@keyframes assistant-orb-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@keyframes assistant-orb-spin-reverse {
  from {
    transform: rotate(360deg);
  }

  to {
    transform: rotate(0deg);
  }
}

@keyframes assistant-orb-sway {
  0%,
  100% {
    transform: translate(0, 0);
  }

  50% {
    transform: translate(-1.5px, 1.5px);
  }
}

@keyframes assistant-symbol-flicker {
  0%,
  100% {
    opacity: 0.82;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.06);
  }
}

@media (max-width: 768px) {
  .assistant-shell {
    pointer-events: none;
  }

  .assistant-trigger,
  .assistant-panel {
    pointer-events: auto;
  }

  .assistant-trigger {
    width: 62px;
    height: 62px;
  }

  .assistant-trigger__orb {
    width: 50px;
    height: 50px;
  }

  .assistant-trigger__symbol {
    inset: 11px;
  }

  .assistant-intro {
    width: min(176px, calc(100vw - 72px));
    padding: 9px 10px;
  }

  .assistant-intro strong {
    font-size: 14px;
  }

  .assistant-intro__meta {
    font-size: 9px;
  }

  .assistant-shell.is-dock-left .assistant-intro {
    left: calc(100% - 6px);
  }

  .assistant-shell.is-dock-right .assistant-intro {
    right: calc(100% - 6px);
  }

  .assistant-panel {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 12px;
    width: auto;
    height: min(80vh, 680px);
    border-radius: 24px;
    --assistant-panel-origin: bottom center;
    --assistant-panel-shift-x: 0px;
  }

  .assistant-shell.is-dock-left .assistant-panel,
  .assistant-shell.is-dock-right .assistant-panel {
    left: 12px;
    right: 12px;
  }

  .assistant-panel__header,
  .assistant-panel__messages,
  .assistant-panel__composer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .assistant-panel__header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .assistant-panel__actions {
    flex-direction: row;
    justify-content: flex-start;
  }

  .assistant-panel__composer textarea {
    min-height: 92px;
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

@media (prefers-reduced-motion: reduce) {
  .assistant-trigger,
  .assistant-panel,
  .assistant-intro,
  .assistant-edge {
    transition: none !important;
  }

  .assistant-trigger__orb,
  .assistant-trigger__orb::after,
  .assistant-trigger__glow--outer,
  .assistant-trigger__glow--inner,
  .assistant-trigger__ring--one,
  .assistant-trigger__ring--two,
  .assistant-trigger__core,
  .assistant-message__meta-status-icon,
  .assistant-message,
  .assistant-message__typing span,
  .assistant-message__pending-line,
  .assistant-message__streaming-pulse,
  .assistant-message__body--pending::after,
  .assistant-message__body--streaming::after {
    animation: none !important;
  }
}
</style>
