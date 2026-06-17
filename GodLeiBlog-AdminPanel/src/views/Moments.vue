<template>
  <div class="moments-manage page-card">
    <section class="moments-summary">
      <div class="moments-summary__item">
        <span>当前页动态</span>
        <strong>{{ moments.length }}</strong>
      </div>
      <div class="moments-summary__item">
        <span>已发布</span>
        <strong>{{ publishedCount }}</strong>
      </div>
      <div class="moments-summary__item">
        <span>草稿/下架</span>
        <strong>{{ draftCount }}</strong>
      </div>
    </section>

    <div class="moments-workbench">
      <section class="moments-list-panel">
        <div class="page-toolbar">
          <el-select v-model="statusFilter" class="toolbar-input" placeholder="状态筛选" @change="handleFilterChange">
            <el-option label="全部状态" value="" />
            <el-option label="已发布" :value="1" />
            <el-option label="草稿 / 下架" :value="0" />
          </el-select>
          <el-button @click="loadMoments">刷新列表</el-button>
          <el-button type="primary" @click="startCreate">新建动态</el-button>
        </div>

        <div class="moments-list">
          <button
            v-for="item in moments"
            :key="item.id"
            class="moment-list-item"
            :class="{ 'is-active': Number(item.id) === Number(selectedId) }"
            type="button"
            @click="selectMoment(item)"
          >
            <div class="moment-list-item__head">
              <strong>{{ summarize(item.content, 22) }}</strong>
              <el-tag :type="item.status === 1 ? 'success' : 'info'">
                {{ item.status === 1 ? '已发布' : '草稿' }}
              </el-tag>
            </div>
            <p>{{ summarize(item.content, 88) }}</p>
            <div class="moment-list-item__meta">
              <span>{{ item.publishTime || '未设置时间' }}</span>
              <span>{{ item.location || '未设置地点' }}</span>
            </div>
          </button>
        </div>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          class="page-pagination"
          @current-change="loadMoments"
        />
      </section>

      <section class="moments-editor-panel">
        <div class="moments-editor-panel__head">
          <div>
            <h3>{{ selectedId ? '编辑动态' : '发布动态' }}</h3>
            <p>在这里维护文案、地点、发布时间、上架状态和图片排序。</p>
          </div>

          <div class="moments-editor-panel__actions">
            <el-button v-if="selectedId" @click="startCreate">切换新建</el-button>
            <el-button type="primary" :loading="saving" @click="saveMoment">保存动态</el-button>
          </div>
        </div>

        <div class="moments-form-grid">
          <div class="moments-form-column">
            <label class="moments-field">
              <span>动态文案</span>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="8"
                maxlength="3000"
                show-word-limit
                placeholder="写一点今天的进展、感受或现场观察..."
              />
            </label>

            <div class="moments-inline-grid">
              <label class="moments-field">
                <span>地点</span>
                <el-input v-model="form.location" placeholder="例如：上海 / 工位 / 咖啡馆" clearable />
              </label>

              <label class="moments-field">
                <span>状态</span>
                <el-select v-model="form.status" placeholder="请选择状态">
                  <el-option label="已发布" :value="1" />
                  <el-option label="草稿 / 下架" :value="0" />
                </el-select>
              </label>
            </div>

            <label class="moments-field">
              <span>发布时间</span>
              <el-date-picker
                v-model="form.publishTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择发布时间"
                style="width: 100%;"
              />
            </label>

            <div class="moments-media-panel">
              <div class="moments-media-panel__head">
                <div>
                  <h4>图片列表</h4>
                  <p>复用现有上传接口，支持多图上传与顺序调整。</p>
                </div>
                <el-button @click="uploadMomentImages">上传图片</el-button>
              </div>

              <div v-if="!form.mediaUrls.length" class="moments-media-empty">
                还没有上传图片，支持单图和多图宫格。
              </div>

              <div v-else class="moments-media-list">
                <div
                  v-for="(image, index) in form.mediaUrls"
                  :key="`${image}-${index}`"
                  class="moments-media-item"
                >
                  <img :src="image" :alt="`moment-media-${index}`" />
                  <div class="moments-media-item__actions">
                    <el-button size="small" @click="moveMedia(index, -1)" :disabled="index === 0">上移</el-button>
                    <el-button size="small" @click="moveMedia(index, 1)" :disabled="index === form.mediaUrls.length - 1">下移</el-button>
                    <el-button size="small" type="danger" plain @click="removeMedia(index)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <aside class="moments-preview-panel">
            <span class="moments-preview-panel__eyebrow">Preview</span>
            <div class="moments-preview-card">
              <div class="moments-preview-card__meta">
                <strong>GodLei Blog</strong>
                <span>{{ form.publishTime || '待设置发布时间' }}</span>
                <em v-if="form.location">{{ form.location }}</em>
              </div>

              <p class="moments-preview-card__content">
                {{ form.content || '这里会实时预览朋友圈动态的文案效果。' }}
              </p>

              <div v-if="form.mediaUrls.length" class="moments-preview-card__gallery">
                <img
                  v-for="(image, index) in form.mediaUrls.slice(0, 4)"
                  :key="`${image}-${index}`"
                  :src="image"
                  :alt="`moment-preview-${index}`"
                />
              </div>
            </div>

            <div v-if="selectedId" class="moments-preview-panel__danger">
              <el-button type="warning" plain @click="toggleStatus">{{ form.status === 1 ? '下架这条动态' : '重新发布这条动态' }}</el-button>
              <el-button type="danger" plain @click="removeSelected">删除动态</el-button>
            </div>
          </aside>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { addMoment, deleteMoment, getMomentList, updateMoment, uploadImage } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const statusFilter = ref('')
const loading = ref(false)
const saving = ref(false)
const selectedId = ref(null)
const moments = ref([])

const form = reactive(createDefaultForm())

const publishedCount = computed(() => moments.value.filter((item) => item.status === 1).length)
const draftCount = computed(() => moments.value.filter((item) => item.status !== 1).length)

function createDefaultForm() {
  return {
    id: null,
    content: '',
    location: '',
    publishTime: formatDateTime(new Date()),
    status: 1,
    mediaUrls: [],
  }
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function summarize(text = '', size = 48) {
  const normalized = String(text || '').trim().replace(/\s+/g, ' ')
  if (!normalized) return '未填写文案'
  return normalized.length > size ? `${normalized.slice(0, size)}...` : normalized
}

function normalizeMoment(item = {}) {
  return {
    id: Number(item.id),
    content: String(item.content || '').trim(),
    location: String(item.location || '').trim(),
    publishTime: item.publishTime || '',
    status: Number(item.status) === 1 ? 1 : 0,
    mediaUrls: Array.isArray(item.mediaUrls) ? item.mediaUrls.filter(Boolean) : [],
    createdAt: item.createdAt || '',
    updatedAt: item.updatedAt || '',
  }
}

function applyForm(next) {
  const payload = next || createDefaultForm()
  form.id = payload.id || null
  form.content = payload.content || ''
  form.location = payload.location || ''
  form.publishTime = payload.publishTime || formatDateTime(new Date())
  form.status = payload.status === 1 ? 1 : 0
  form.mediaUrls = Array.isArray(payload.mediaUrls) ? [...payload.mediaUrls] : []
}

async function loadMoments() {
  if (loading.value) return
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
    }
    if (statusFilter.value !== '') {
      params.status = statusFilter.value
    }

    const data = await getMomentList(params)
    const rows = Array.isArray(data?.rows) ? data.rows.map(normalizeMoment) : []
    moments.value = rows
    total.value = Number(data?.total || 0)

    if (!selectedId.value && rows.length) {
      selectMoment(rows[0])
    } else if (selectedId.value) {
      const current = rows.find((item) => Number(item.id) === Number(selectedId.value))
      if (current) {
        selectMoment(current)
      }
    }
  } catch (error) {
    console.error('加载朋友圈列表失败', error)
    ElMessage.error(error.message || '加载朋友圈列表失败')
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  loadMoments()
}

function startCreate() {
  selectedId.value = null
  applyForm(createDefaultForm())
}

function selectMoment(item) {
  selectedId.value = item.id
  applyForm(item)
}

function buildPayload() {
  return {
    id: form.id || undefined,
    content: form.content.trim(),
    location: form.location.trim(),
    publishTime: form.publishTime,
    status: form.status,
    mediaUrls: [...form.mediaUrls],
  }
}

async function saveMoment() {
  if (!form.content.trim()) {
    ElMessage.warning('请先填写动态文案')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    if (selectedId.value) {
      await updateMoment(payload)
      ElMessage.success('动态已更新')
    } else {
      await addMoment(payload)
      ElMessage.success('动态已发布')
      startCreate()
    }
    await loadMoments()
  } catch (error) {
    console.error('保存动态失败', error)
    ElMessage.error(error.message || '保存动态失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus() {
  if (!selectedId.value) return
  form.status = form.status === 1 ? 0 : 1
  await saveMoment()
}

function removeSelected() {
  if (!selectedId.value) return
  const current = moments.value.find((item) => Number(item.id) === Number(selectedId.value))
  if (!current) return
  removeMoment(current)
}

function removeMoment(item) {
  ElMessageBox.confirm(`确定删除这条动态吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await deleteMoment({ id: item.id })
      ElMessage.success('动态已删除')
      if (Number(selectedId.value) === Number(item.id)) {
        startCreate()
      }
      await loadMoments()
    })
    .catch(() => {})
}

async function uploadMomentImages() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.multiple = true
  input.click()

  input.onchange = async () => {
    const files = Array.from(input.files || [])
    if (!files.length) return

    try {
      for (const file of files) {
        const result = await uploadImage(file, 'moment-media')
        if (result?.url) {
          form.mediaUrls.push(result.url)
        }
      }
      ElMessage.success('图片已加入动态')
    } catch (error) {
      console.error('上传动态图片失败', error)
      ElMessage.error(error.message || '上传动态图片失败')
    }
  }
}

function moveMedia(index, delta) {
  const targetIndex = index + delta
  if (targetIndex < 0 || targetIndex >= form.mediaUrls.length) return
  const next = [...form.mediaUrls]
  const [current] = next.splice(index, 1)
  next.splice(targetIndex, 0, current)
  form.mediaUrls = next
}

function removeMedia(index) {
  form.mediaUrls.splice(index, 1)
}

onMounted(() => {
  loadMoments()
})
</script>

<style scoped>
.moments-manage {
  padding: 18px;
}

.moments-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.moments-summary__item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--admin-border-soft);
  background: rgba(255, 255, 255, 0.74);
}

.moments-summary__item span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
  margin-bottom: 8px;
}

.moments-summary__item strong {
  font-size: 28px;
  color: var(--admin-text);
}

.moments-workbench {
  display: grid;
  grid-template-columns: minmax(320px, 0.72fr) minmax(0, 1.28fr);
  gap: 18px;
}

.moments-list-panel,
.moments-editor-panel {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid var(--admin-border-soft);
  background: rgba(255, 255, 255, 0.72);
}

.moments-list {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.moment-list-item {
  width: 100%;
  text-align: left;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(214, 173, 92, 0.12);
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
}

.moment-list-item.is-active {
  border-color: var(--admin-border-strong);
  box-shadow: 0 12px 24px rgba(112, 84, 34, 0.08);
}

.moment-list-item__head,
.moment-list-item__meta,
.moments-editor-panel__head,
.moments-editor-panel__actions,
.moments-media-panel__head,
.moments-media-item__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.moment-list-item p {
  margin: 10px 0;
  color: var(--admin-text-muted);
  line-height: 1.7;
}

.moment-list-item__meta {
  color: var(--admin-text-soft);
  font-size: 12px;
}

.moments-editor-panel__head {
  margin-bottom: 16px;
}

.moments-editor-panel__head h3,
.moments-media-panel__head h4 {
  margin: 0;
}

.moments-editor-panel__head p,
.moments-media-panel__head p {
  margin: 6px 0 0;
  color: var(--admin-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.moments-form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 18px;
}

.moments-form-column {
  display: grid;
  gap: 16px;
}

.moments-inline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.moments-field {
  display: grid;
  gap: 8px;
}

.moments-field span {
  color: var(--admin-text);
  font-size: 13px;
  font-weight: 600;
}

.moments-media-panel,
.moments-preview-card {
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(214, 173, 92, 0.12);
  background: rgba(255, 255, 255, 0.72);
}

.moments-media-empty {
  padding: 18px 0 6px;
  color: var(--admin-text-soft);
}

.moments-media-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.moments-media-item {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.moments-media-item img,
.moments-preview-card__gallery img {
  width: 100%;
  border-radius: 14px;
  object-fit: cover;
}

.moments-media-item img {
  height: 88px;
}

.moments-preview-panel {
  display: grid;
  gap: 14px;
  align-content: start;
}

.moments-preview-panel__eyebrow {
  color: var(--admin-text-soft);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.moments-preview-card__meta {
  display: grid;
  gap: 4px;
}

.moments-preview-card__meta span,
.moments-preview-card__meta em {
  color: var(--admin-text-muted);
  font-style: normal;
  font-size: 13px;
}

.moments-preview-card__content {
  margin: 16px 0;
  white-space: pre-wrap;
  line-height: 1.8;
}

.moments-preview-card__gallery {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.moments-preview-card__gallery img {
  height: 110px;
}

.moments-preview-panel__danger {
  display: grid;
  gap: 10px;
}

@media (max-width: 1200px) {
  .moments-workbench,
  .moments-form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .moments-manage {
    padding: 12px;
  }

  .moments-summary,
  .moments-inline-grid,
  .moments-media-item {
    grid-template-columns: 1fr;
  }

  .moments-editor-panel__head,
  .moments-editor-panel__actions,
  .moments-media-panel__head,
  .moments-media-item__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
