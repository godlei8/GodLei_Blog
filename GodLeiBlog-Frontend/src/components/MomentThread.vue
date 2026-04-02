<template>
  <div class="moment-thread" :class="{ 'is-expanded': expanded }">
    <button class="moment-thread__toggle" type="button" :disabled="loading" @click="toggleThread">
      {{ toggleLabel }}
    </button>

    <transition name="moment-thread-panel">
      <div v-show="expanded" class="moment-thread__panel">
        <div ref="container" class="moment-thread__embed"></div>
        <p v-if="errorMessage" class="moment-thread__error">{{ errorMessage }}</p>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { getTwikooEnvId, loadTwikoo } from '@/utils/twikoo'

const props = defineProps({
  momentId: {
    type: Number,
    required: true,
  },
})

const expanded = ref(false)
const loading = ref(false)
const initialized = ref(false)
const errorMessage = ref('')
const container = ref(null)

const toggleLabel = computed(() => {
  if (loading.value) return '加载评论中...'
  return expanded.value ? '收起评论' : '展开评论'
})

async function initThread() {
  if (initialized.value) return
  await nextTick()

  const el = container.value
  if (!el) return

  loading.value = true
  errorMessage.value = ''

  try {
    const twikoo = await loadTwikoo()
    await Promise.resolve(
      twikoo.init({
        envId: getTwikooEnvId(),
        el,
        path: `/moments/${props.momentId}`,
      })
    )
    initialized.value = true
  } catch (error) {
    console.error('Moments comment thread init failed', error)
    errorMessage.value = `评论加载失败：${error?.message || '请稍后重试'}`
  } finally {
    loading.value = false
  }
}

async function toggleThread() {
  if (loading.value) return
  expanded.value = !expanded.value
  if (expanded.value) {
    await initThread()
  }
}
</script>

<style scoped>
.moment-thread {
  display: grid;
  gap: 12px;
}

.moment-thread__toggle {
  width: fit-content;
  border-radius: 999px;
  padding: 9px 16px;
  font-size: 13px;
  line-height: 1;
  transition: border-color 0.2s ease, transform 0.2s ease, color 0.2s ease, background 0.2s ease;
  border: 1px solid var(--theme-accent-border);
  background: linear-gradient(135deg, rgba(122, 29, 45, 0.78), rgba(214, 173, 92, 0.42));
  color: var(--theme-accent-text-strong);
  cursor: pointer;
}

.moment-thread__toggle:hover {
  transform: translateY(-1px);
}

.moment-thread__toggle:hover {
  border-color: var(--theme-accent-border-strong);
}

.moment-thread__toggle:disabled {
  opacity: 0.7;
  cursor: wait;
  transform: none;
}

.moment-thread__panel {
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(255, 247, 234, 0.08);
  background: rgba(0, 0, 0, 0.28);
}

.moment-thread__error {
  margin: 12px 0 0;
  color: #f7c7b2;
  font-size: 13px;
}

.moment-thread-panel-enter-active,
.moment-thread-panel-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.moment-thread-panel-enter-from,
.moment-thread-panel-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.moment-thread__panel :deep(.tk-comments) {
  color: var(--theme-accent-text-strong);
}

.moment-thread__panel :deep(.tk-main) {
  gap: 14px;
}

.moment-thread__panel :deep(.tk-content) {
  color: rgba(255, 247, 234, 0.9);
  line-height: 1.8;
}

.moment-thread__panel :deep(.tk-submit),
.moment-thread__panel :deep(.tk-comment),
.moment-thread__panel :deep(.tk-preview) {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  box-shadow: none;
}

.moment-thread__panel :deep(.tk-comment) {
  padding: 14px 14px 12px;
}

.moment-thread__panel :deep(.tk-row) {
  gap: 10px;
}

.moment-thread__panel :deep(.tk-avatar) {
  border-radius: 14px;
}

.moment-thread__panel :deep(.tk-meta) {
  color: rgba(255, 247, 234, 0.62);
}

.moment-thread__panel :deep(.tk-send),
.moment-thread__panel :deep(.tk-button),
.moment-thread__panel :deep(.tk-submit-action-icon) {
  background: linear-gradient(135deg, rgba(122, 29, 45, 0.92), rgba(214, 173, 92, 0.88));
  border: none;
  border-radius: 999px;
  color: #fff7ea;
}

.moment-thread__panel :deep(textarea),
.moment-thread__panel :deep(input),
.moment-thread__panel :deep(.tk-input) {
  background: rgba(15, 8, 10, 0.68);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #fff7ea;
}

.moment-thread__panel :deep(.tk-expand) {
  color: var(--theme-accent-text);
}

@media (max-width: 768px) {
  .moment-thread__toggle {
    width: 100%;
    justify-self: stretch;
  }
}
</style>
