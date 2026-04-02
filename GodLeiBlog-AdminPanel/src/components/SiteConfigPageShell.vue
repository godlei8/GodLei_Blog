<template>
  <div class="site-settings page-card">
    <section class="settings-hero">
      <div class="settings-hero__copy">
        <span class="settings-hero__eyebrow">{{ eyebrow }}</span>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>

        <div class="settings-hero__meta">
          <span class="settings-status" :class="{ 'is-dirty': isDirty, 'is-busy': loading || saving }">
            {{ statusText }}
          </span>
          <span class="settings-hero__meta-text">{{ statusDescription }}</span>
        </div>
      </div>

      <div class="settings-hero__actions">
        <slot name="actions" />
      </div>
    </section>

    <section v-if="summaryItems.length" class="settings-summary">
      <div v-for="item in summaryItems" :key="item.label" class="settings-summary__card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <div class="settings-layout">
      <div class="settings-editor">
        <slot />
      </div>
    </div>

    <section v-if="hasPreview" class="settings-preview">
      <slot name="preview" />
    </section>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

const slots = useSlots()
const hasPreview = computed(() => Boolean(slots.preview))

defineProps({
  eyebrow: {
    type: String,
    default: 'Settings'
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  statusText: {
    type: String,
    default: ''
  },
  statusDescription: {
    type: String,
    default: ''
  },
  isDirty: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  saving: {
    type: Boolean,
    default: false
  },
  summaryItems: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.site-settings {
  padding: 16px;
}

.settings-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--admin-border-soft);
}

.settings-hero__eyebrow,
.settings-preview__eyebrow {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(214, 173, 92, 0.14);
  color: #8a6427;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.settings-hero h2 {
  margin: 10px 0 6px;
  font-size: 28px;
}

.settings-hero p {
  margin: 0;
  color: var(--admin-text-muted);
  line-height: 1.7;
}

.settings-hero__meta {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
  flex-wrap: wrap;
}

.settings-status {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--admin-text);
  font-size: 13px;
  font-weight: 600;
}

.settings-status.is-dirty {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.settings-status.is-busy {
  background: rgba(214, 173, 92, 0.18);
  color: #8a6427;
}

.settings-hero__meta-text {
  color: var(--admin-text-muted);
  font-size: 13px;
}

.settings-hero__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  align-content: start;
}

.settings-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 14px 0;
}

.settings-summary__card,
.settings-section,
.settings-preview :deep(.settings-preview__card) {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--admin-border-soft);
  background: rgba(255, 255, 255, 0.9);
}

.settings-summary__card span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
  margin-bottom: 8px;
}

.settings-summary__card strong {
  font-size: 24px;
  color: var(--admin-text);
}

.settings-layout {
  display: grid;
  gap: 14px;
}

.settings-editor {
  display: grid;
  gap: 14px;
}

.settings-preview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
  margin-top: 14px;
  align-items: start;
}

.settings-preview :deep(.settings-preview__card h3) {
  margin: 12px 0;
  font-size: 22px;
}

@media (max-width: 900px) {
  .settings-hero,
  .settings-summary {
    grid-template-columns: 1fr;
  }

  .settings-hero__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .site-settings {
    padding: 10px;
  }

  .settings-summary__card strong {
    font-size: 22px;
  }

  .settings-summary,
  .settings-layout,
  .settings-preview {
    gap: 12px;
  }
}
</style>
