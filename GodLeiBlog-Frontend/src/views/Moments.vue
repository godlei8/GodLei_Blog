<template>
  <section class="moments-page">
    <div class="moments-shell">
      <header class="moments-hero">
        <div class="moments-hero__copy">
          <span class="moments-hero__eyebrow">Moments</span>
          <h1>朋友圈</h1>
          <p>这里记录博客站点的即时动态、碎片想法和生活现场，每条动态都拥有独立评论线程。</p>
        </div>

        <div class="moments-hero__meta">
          <div class="moments-hero__stat">
            <strong>{{ total }}</strong>
            <span>已发布动态</span>
          </div>
          <div class="moments-hero__stat">
            <strong>{{ moments.length }}</strong>
            <span>当前已加载</span>
          </div>
        </div>
      </header>

      <div v-if="loading && !moments.length" class="moments-empty-card">
        正在加载动态流...
      </div>

      <div v-else-if="loadError" class="moments-empty-card">
        <p>{{ loadError }}</p>
        <button class="moments-loadmore__button" type="button" @click="loadMoments(true)">重新加载</button>
      </div>

      <div v-else-if="!moments.length" class="moments-empty-card">
        暂时还没有公开的朋友圈动态。
      </div>

      <div v-else class="moments-list">
        <article
          v-for="moment in moments"
          :id="getMomentAnchorId(moment.id)"
          :key="moment.id"
          class="moment-card"
          :class="{ 'is-targeted': Number(moment.id) === Number(targetMomentId) }"
        >
          <div class="moment-card__head">
            <img
              class="moment-card__avatar"
              :src="profileAvatar"
              :alt="siteName"
            >

            <div class="moment-card__meta">
              <strong>{{ siteName }}</strong>
              <span>{{ formatPublishTime(moment.publishTime) }}</span>
              <em v-if="moment.location">{{ moment.location }}</em>
            </div>
          </div>

          <p class="moment-card__content">{{ moment.content }}</p>

          <div
            v-if="moment.mediaList?.length"
            class="moment-card__gallery"
            :class="`is-${Math.min(moment.mediaList.length, 9)}`"
          >
            <button
              v-for="(image, index) in moment.mediaList"
              :key="`${moment.id}-${index}`"
              class="moment-card__image"
              type="button"
              @click="openPreview(moment.mediaList, index)"
            >
              <img :src="resolveImageUrl(image, coverFallbackUrl)" :alt="`moment-${moment.id}-${index}`">
            </button>
          </div>

          <footer class="moment-card__footer">
            <MomentThread :moment-id="Number(moment.id)" />
          </footer>
        </article>
      </div>

      <div v-if="moments.length && hasMore" class="moments-loadmore">
        <button class="moments-loadmore__button" type="button" :disabled="loading" @click="loadMoments(false)">
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>

    <transition name="moments-preview">
      <div v-if="preview.visible" class="moments-preview" @click.self="closePreview">
        <button class="moments-preview__close" type="button" @click="closePreview">关闭</button>
        <button
          v-if="preview.images.length > 1"
          class="moments-preview__nav moments-preview__nav--prev"
          type="button"
          @click.stop="stepPreview(-1)"
        >
          上一张
        </button>
        <img
          class="moments-preview__image"
          :src="resolveImageUrl(preview.images[preview.index], coverFallbackUrl)"
          alt="moment preview"
        >
        <button
          v-if="preview.images.length > 1"
          class="moments-preview__nav moments-preview__nav--next"
          type="button"
          @click.stop="stepPreview(1)"
        >
          下一张
        </button>
      </div>
    </transition>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { fetchMomentsList } from '@/api'
import MomentThread from '@/components/MomentThread.vue'
import { avatarFallbackUrl, coverFallbackUrl, normalizeMediaUrl, resolveImageUrl } from '@/utils/image'
import { setPageContext } from '@/utils/pageContext'
import { getDefaultSiteConfig, loadSiteConfig } from '@/utils/siteConfig'

const loading = ref(false)
const loadError = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const moments = ref([])
const targetMomentId = ref(null)
const siteConfig = ref(getDefaultSiteConfig())

const preview = reactive({
  visible: false,
  images: [],
  index: 0,
})

const hasMore = computed(() => moments.value.length < total.value)
const siteName = computed(() => siteConfig.value.basic?.siteName || 'GodLei Blog')
const profileAvatar = computed(() => resolveImageUrl(siteConfig.value.basic?.profileAvatar, avatarFallbackUrl))

function unwrapResult(payload) {
  if (payload?.data) return payload.data
  return payload || {}
}

function normalizeMomentItem(item = {}) {
  const mediaList = Array.isArray(item.mediaList)
    ? item.mediaList.map((image) => normalizeMediaUrl(image)).filter(Boolean)
    : []

  return {
    id: Number(item.id),
    content: String(item.content || '').trim(),
    location: String(item.location || '').trim(),
    publishTime: item.publishTime || '',
    mediaList,
  }
}

function formatPublishTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function getMomentAnchorId(id) {
  return `moment-${Number(id)}`
}

function readTargetMomentIdFromHash() {
  if (typeof window === 'undefined') return null
  const rawHash = decodeURIComponent(window.location.hash || '').replace(/^#/, '').trim()
  const match = rawHash.match(/^moment-(\d+)$/)
  return match ? Number(match[1]) : null
}

function syncPageContext() {
  const summaryLines = moments.value.slice(0, 3).map((item) => {
    const content = item.content.length > 60 ? `${item.content.slice(0, 60)}...` : item.content
    return `${formatPublishTime(item.publishTime)} ${item.location ? `[${item.location}] ` : ''}${content}`
  })

  setPageContext({
    pageType: 'moments',
    route: '/moments',
    title: '朋友圈',
    summary: '博客站点的动态流页面，展示最近发布的文字、图片和评论入口。',
    contentExcerpt: summaryLines.join('\n'),
    currentMomentSummary: summaryLines[0] || '',
  })
}

async function loadConfig() {
  siteConfig.value = await loadSiteConfig()
}

async function loadMoments(reset = false) {
  if (loading.value) return
  loading.value = true
  loadError.value = ''

  try {
    if (reset) {
      page.value = 1
    }

    const payload = await fetchMomentsList(page.value, pageSize.value, 1)
    const data = unwrapResult(payload)
    const rows = Array.isArray(data.rows) ? data.rows.map(normalizeMomentItem) : []

    total.value = Number(data.total || 0)
    moments.value = reset ? rows : moments.value.concat(rows)

    if (!reset) {
      page.value += 1
    } else {
      page.value = rows.length ? 2 : 1
    }

    syncPageContext()
  } catch (error) {
    console.error('Failed to load moments list', error)
    loadError.value = error?.message || '加载动态失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function ensureTargetMomentLoaded(momentId) {
  if (!momentId) return false
  let guard = 0

  while (!moments.value.some((item) => Number(item.id) === Number(momentId)) && hasMore.value && guard < 24) {
    guard += 1
    await loadMoments(false)
  }

  return moments.value.some((item) => Number(item.id) === Number(momentId))
}

async function syncMomentTargetFromHash({ smooth = true } = {}) {
  const nextTargetId = readTargetMomentIdFromHash()
  targetMomentId.value = nextTargetId

  if (!nextTargetId) return

  await ensureTargetMomentLoaded(nextTargetId)
  await nextTick()

  const target = document.getElementById(getMomentAnchorId(nextTargetId))
  if (!target) return
  target.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto', block: 'start' })
}

function openPreview(images = [], index = 0) {
  preview.images = images
  preview.index = index
  preview.visible = true
}

function closePreview() {
  preview.visible = false
}

function stepPreview(direction) {
  const size = preview.images.length
  if (!size) return
  preview.index = (preview.index + direction + size) % size
}

function handleHashChange() {
  syncMomentTargetFromHash()
}

onMounted(async () => {
  window.addEventListener('hashchange', handleHashChange)
  await Promise.allSettled([
    loadConfig(),
    loadMoments(true),
  ])
  await syncMomentTargetFromHash({ smooth: false })
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', handleHashChange)
})
</script>

<style scoped>
.moments-page {
  width: 100%;
  color: var(--theme-accent-text-strong);
}

.moments-shell {
  width: min(1040px, calc(100% - 24px));
  margin: 32px auto 60px;
}

.moments-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
  gap: 20px;
  padding: 28px;
  border-radius: 28px;
  border: 1px solid var(--theme-accent-border-soft);
  background: linear-gradient(135deg, rgba(11, 5, 8, 0.92), rgba(61, 15, 27, 0.78) 54%, rgba(214, 173, 92, 0.18));
  box-shadow: 0 24px 40px rgba(12, 3, 5, 0.36);
}

.moments-hero__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 247, 234, 0.08);
  color: var(--theme-accent-soft);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 12px;
}

.moments-hero h1 {
  margin: 14px 0 10px;
  font-size: clamp(34px, 5vw, 54px);
  line-height: 1;
}

.moments-hero p {
  margin: 0;
  color: rgba(255, 247, 234, 0.78);
  line-height: 1.8;
}

.moments-hero__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-content: end;
}

.moments-hero__stat {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.moments-hero__stat strong {
  display: block;
  font-size: 28px;
  color: var(--theme-accent-text);
}

.moments-hero__stat span {
  display: block;
  margin-top: 6px;
  color: rgba(255, 247, 234, 0.64);
  font-size: 13px;
}

.moments-list {
  display: grid;
  gap: 18px;
  margin-top: 22px;
}

.moment-card,
.moments-empty-card {
  padding: 22px;
  border-radius: 26px;
  border: 1px solid var(--theme-accent-border-soft);
  background: linear-gradient(180deg, rgba(15, 8, 10, 0.92), rgba(7, 3, 5, 0.88));
  box-shadow: 0 18px 34px rgba(12, 3, 5, 0.28);
}

.moment-card {
  scroll-margin-top: 28px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.moment-card.is-targeted {
  border-color: rgba(214, 173, 92, 0.52);
  box-shadow: 0 24px 44px rgba(214, 173, 92, 0.18);
}

.moments-empty-card {
  margin-top: 22px;
  text-align: center;
  color: rgba(255, 247, 234, 0.72);
}

.moments-empty-card p {
  margin: 0 0 14px;
}

.moment-card__head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.moment-card__avatar {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid var(--theme-accent-border);
}

.moment-card__meta {
  display: grid;
  gap: 4px;
}

.moment-card__meta strong {
  font-size: 18px;
}

.moment-card__meta span,
.moment-card__meta em {
  font-style: normal;
  color: rgba(255, 247, 234, 0.64);
  font-size: 13px;
}

.moment-card__content {
  margin: 18px 0 0;
  white-space: pre-wrap;
  line-height: 1.9;
  color: rgba(255, 247, 234, 0.92);
}

.moment-card__gallery {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.moment-card__gallery.is-1 {
  grid-template-columns: minmax(0, 1fr);
}

.moment-card__gallery.is-2,
.moment-card__gallery.is-4 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.moment-card__gallery.is-3,
.moment-card__gallery.is-5,
.moment-card__gallery.is-6,
.moment-card__gallery.is-7,
.moment-card__gallery.is-8,
.moment-card__gallery.is-9 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.moment-card__image {
  padding: 0;
  border: 0;
  border-radius: 18px;
  overflow: hidden;
  background: transparent;
  cursor: pointer;
}

.moment-card__image img {
  display: block;
  width: 100%;
  height: 220px;
  object-fit: cover;
  transition: transform 0.25s ease;
}

.moment-card__image:hover img {
  transform: scale(1.03);
}

.moment-card__footer {
  margin-top: 20px;
}

.moments-loadmore {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.moments-loadmore__button {
  border: 1px solid var(--theme-accent-border);
  border-radius: 999px;
  background: rgba(255, 247, 234, 0.08);
  color: var(--theme-accent-text-strong);
  padding: 10px 18px;
  cursor: pointer;
}

.moments-loadmore__button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.moments-preview {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.88);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
}

.moments-preview__image {
  width: min(88vw, 980px);
  max-height: 84vh;
  object-fit: contain;
}

.moments-preview__close,
.moments-preview__nav {
  position: fixed;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  background: rgba(15, 8, 10, 0.72);
  color: #fff7ea;
  padding: 10px 16px;
  cursor: pointer;
}

.moments-preview__close {
  top: 24px;
  right: 24px;
}

.moments-preview__nav--prev {
  left: 24px;
}

.moments-preview__nav--next {
  right: 24px;
}

.moments-preview-enter-active,
.moments-preview-leave-active {
  transition: opacity 0.2s ease;
}

.moments-preview-enter-from,
.moments-preview-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .moments-shell {
    width: min(100%, calc(100% - 20px));
    margin-top: 18px;
  }

  .moments-hero {
    grid-template-columns: 1fr;
    padding: 22px;
  }

  .moments-hero__meta {
    grid-template-columns: 1fr 1fr;
  }

  .moment-card,
  .moments-empty-card {
    padding: 18px;
  }

  .moment-card__gallery,
  .moment-card__gallery.is-2,
  .moment-card__gallery.is-3,
  .moment-card__gallery.is-4,
  .moment-card__gallery.is-5,
  .moment-card__gallery.is-6,
  .moment-card__gallery.is-7,
  .moment-card__gallery.is-8,
  .moment-card__gallery.is-9 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .moment-card__gallery.is-1 {
    grid-template-columns: 1fr;
  }

  .moment-card__image img {
    height: 180px;
  }

  .moments-preview__nav {
    bottom: 24px;
    top: auto;
  }

  .moments-preview__nav--prev {
    left: 24px;
  }

  .moments-preview__nav--next {
    right: 24px;
  }
}
</style>
