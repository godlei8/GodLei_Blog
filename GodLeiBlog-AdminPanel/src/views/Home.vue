<template>
  <div class="dashboard">
    <div class="dash-controls">
      <span class="dash-controls__hint">全站访问流量与最新互动</span>
      <span class="spacer"></span>
      <el-select v-model="days" size="small" class="days-select" @change="loadDashboard">
        <el-option :value="7" label="最近 7 天" />
        <el-option :value="14" label="最近 14 天" />
        <el-option :value="30" label="最近 30 天" />
      </el-select>
      <el-button size="small" :loading="loadingDashboard" @click="loadDashboard">刷新流量</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat">
        <div class="stat__label">总访问量 · PV</div>
        <div class="stat__value">{{ (pageViews || 0).toLocaleString() }}</div>
        <div class="stat__hint">今日 PV {{ todayPoint.pageViews || 0 }}</div>
      </div>
      <div class="stat">
        <div class="stat__label">总访客量 · UV</div>
        <div class="stat__value">{{ (uniqueVisitors || 0).toLocaleString() }}</div>
        <div class="stat__hint">今日 UV {{ todayPoint.uniqueVisitors || 0 }}</div>
      </div>
      <div class="stat">
        <div class="stat__label">今日访问 · PV</div>
        <div class="stat__value">{{ todayPoint.pageViews || 0 }}</div>
        <div class="stat__hint">最近 {{ days }} 天趋势见下方</div>
      </div>
      <div class="stat">
        <div class="stat__label">今日访客 · UV</div>
        <div class="stat__value">{{ todayPoint.uniqueVisitors || 0 }}</div>
        <div class="stat__hint">最近 {{ days }} 天趋势见下方</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel">
        <div class="panel__head"><h2>PV 历史（按天）</h2></div>
        <div class="panel__body"><div ref="pvChartRef" class="chart"></div></div>
      </div>
      <div class="panel">
        <div class="panel__head"><h2>UV 历史（按天）</h2></div>
        <div class="panel__body"><div ref="uvChartRef" class="chart"></div></div>
      </div>
    </div>

    <div class="panel">
      <div class="panel__head">
        <h2>最新评论</h2>
        <el-button type="text" size="small" :loading="loadingComments" @click="loadLatestComments">刷新</el-button>
      </div>
      <div class="panel__body">
        <div v-if="loadingComments" class="loading-wrap">
          <el-skeleton :rows="6" animated />
        </div>
        <div v-else-if="commentsError" class="comments-error">{{ commentsError }}</div>
        <el-table v-else :data="latestComments" height="300" style="width: 100%;" empty-text="暂无最新评论数据">
          <el-table-column prop="nick" label="昵称" width="130" />
          <el-table-column prop="timeText" label="时间" width="190" />
          <el-table-column prop="page" label="页面" width="180" />
          <el-table-column label="内容">
            <template #default="scope">
              <div class="comment-text" :title="scope.row.text">{{ scope.row.text }}</div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { getAdminDashboardStats, getMomentList, getPostList } from '@/api'

const PROD_TWIKOO_URL = 'https://twikoo.godlei.cn'
const SAME_ORIGIN_TWIKOO_PROXY = '/twikoo-proxy/'
const TWIKOO_UNAVAILABLE_HINT = '当前 Twikoo 服务地址不可用，请检查 VITE_TWIKOO_URL 或 /twikoo-proxy 的反向代理目标。'
const TWIKOO_RATE_LIMIT_HINT = 'Twikoo 评论当前返回 Too Many Requests，请确认服务端已关闭限流或稍后再试。'
const TWIKOO_LOCAL_SERVICE_HINT = '如果是本地开发，请确认本地 Twikoo 服务已启动，默认地址为 http://127.0.0.1:3000 。'

function compactTwikooDetail(detail) {
  return String(detail || '')
    .replace(/\s+/g, ' ')
    .replace(/<[^>]+>/g, ' ')
    .trim()
    .slice(0, 200)
}

function formatTwikooTarget(target) {
  const raw = String(target || '').trim()
  if (!raw) return '未配置'
  try {
    const parsed = new URL(raw)
    return `${parsed.origin}${parsed.pathname}`.replace(/\/$/, '') || parsed.origin
  } catch {
    return raw
  }
}

function resolveTwikooTarget(target) {
  const raw = String(target || '').trim()
  if (!raw) return ''
  if (typeof window === 'undefined') return raw
  try {
    return new URL(raw, window.location.origin).toString()
  } catch {
    return raw
  }
}

function isLocalHostname(hostname = '') {
  return ['localhost', '127.0.0.1', '::1'].includes(String(hostname || '').toLowerCase())
}

function shouldShowLocalTwikooHint(target = '') {
  try {
    const parsed = new URL(target)
    return isLocalHostname(parsed.hostname) && parsed.pathname.startsWith('/twikoo-proxy')
  } catch {
    return false
  }
}

function buildTwikooRequestErrorMessage(status, detail = '', target = '') {
  const compactDetail = compactTwikooDetail(detail)
  const isRateLimited =
    Number(status) === 429
    || /Too Many Requests/i.test(compactDetail)
  const isUnavailable =
    Number(status) === 405
    || (Number(status) === 500 && (!compactDetail || /proxy|ECONNREFUSED|connect ECONNREFUSED|socket hang up|fetch failed/i.test(compactDetail)))
    || /405 Not Allowed/i.test(compactDetail)
    || /站点已暂停|停止运行|nginx|ECONNREFUSED|connect ECONNREFUSED|Error occurred while trying to proxy/i.test(compactDetail)

  if (isRateLimited) {
    return `${TWIKOO_RATE_LIMIT_HINT} 当前目标：${formatTwikooTarget(target)}`
  }

  if (isUnavailable) {
    const localHint = shouldShowLocalTwikooHint(target) ? ` ${TWIKOO_LOCAL_SERVICE_HINT}` : ''
    return `${TWIKOO_UNAVAILABLE_HINT}${localHint} 当前目标：${formatTwikooTarget(target)}`
  }

  return `Twikoo 请求失败：${status}${compactDetail ? ` ${compactDetail}` : ''}`
}

function isTwikooServiceUnavailableError(message) {
  return String(message || '').includes(TWIKOO_UNAVAILABLE_HINT) || String(message || '').includes('Twikoo 服务当前不可用')
}

export default {
  name: 'Home',
  data() {
    return {
      days: 14,

      loadingDashboard: false,
      loadingComments: false,

      pageViews: 0,
      uniqueVisitors: 0,
      history: [],

      pvChart: null,
      uvChart: null,

      latestComments: [],
      commentsError: '',
    }
  },
  computed: {
    todayPoint() {
      const list = Array.isArray(this.history) ? this.history : []
      if (!list.length) return { pageViews: 0, uniqueVisitors: 0 }
      const last = list[list.length - 1] || {}
      return {
        pageViews: last.pageViews || 0,
        uniqueVisitors: last.uniqueVisitors || 0,
      }
    },
  },
  mounted() {
    this.loadDashboard()
    this.loadLatestComments()
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    if (this.pvChart) this.pvChart.dispose()
    if (this.uvChart) this.uvChart.dispose()
  },
  methods: {
    async loadDashboard() {
      this.loadingDashboard = true
      try {
        const data = await getAdminDashboardStats({ days: this.days })
        this.pageViews = data?.pageViews || 0
        this.uniqueVisitors = data?.uniqueVisitors || 0
        this.history = Array.isArray(data?.history) ? data.history : []
        this.renderCharts()
      } catch (e) {
        console.error('加载仪表盘流量失败:', e)
        this.$message.error('加载流量数据失败')
      } finally {
        this.loadingDashboard = false
      }
    },

    renderCharts() {
      if (!window.echarts) return
      if (!Array.isArray(this.history) || !this.history.length) return

      const pvEl = this.$refs.pvChartRef
      const uvEl = this.$refs.uvChartRef
      if (!pvEl || !uvEl) return

      if (!this.pvChart) this.pvChart = window.echarts.init(pvEl)
      if (!this.uvChart) this.uvChart = window.echarts.init(uvEl)

      const xData = this.history.map((i) => i.date)
      const pvData = this.history.map((i) => i.pageViews || 0)
      const uvData = this.history.map((i) => i.uniqueVisitors || 0)

      const axisLabel = { color: '#9a958a', fontFamily: 'JetBrains Mono, monospace', fontSize: 10 }
      const splitLine = { lineStyle: { color: '#efece4' } }
      const baseGrid = { left: 6, right: 12, bottom: 0, top: 12, containLabel: true }

      const pvOption = {
        tooltip: { trigger: 'axis' },
        grid: baseGrid,
        xAxis: { type: 'category', data: xData, boundaryGap: false, axisTick: { show: false }, axisLine: { lineStyle: { color: '#e7e3da' } }, axisLabel },
        yAxis: { type: 'value', minInterval: 1, splitLine, axisLabel },
        series: [
          {
            name: 'PV',
            type: 'line',
            data: pvData,
            smooth: true,
            symbol: 'circle',
            symbolSize: 5,
            lineStyle: { width: 2.4, color: '#a9772b' },
            itemStyle: { color: '#a9772b', borderColor: '#fff', borderWidth: 1.5 },
            areaStyle: { color: 'rgba(169, 119, 43, 0.12)' },
          },
        ],
      }

      const uvOption = {
        tooltip: { trigger: 'axis' },
        grid: baseGrid,
        xAxis: { type: 'category', data: xData, boundaryGap: false, axisTick: { show: false }, axisLine: { lineStyle: { color: '#e7e3da' } }, axisLabel },
        yAxis: { type: 'value', minInterval: 1, splitLine, axisLabel },
        series: [
          {
            name: 'UV',
            type: 'line',
            data: uvData,
            smooth: true,
            symbol: 'circle',
            symbolSize: 5,
            lineStyle: { width: 2.4, color: '#7a1d2d' },
            itemStyle: { color: '#7a1d2d', borderColor: '#fff', borderWidth: 1.5 },
            areaStyle: { color: 'rgba(122, 29, 45, 0.10)' },
          },
        ],
      }

      this.pvChart.setOption(pvOption, true)
      this.uvChart.setOption(uvOption, true)
    },

    handleResize() {
      try {
        if (this.pvChart) this.pvChart.resize()
        if (this.uvChart) this.uvChart.resize()
      } catch (_) {}
    },

    getTwikooEnvId() {
      const fromEnv = import.meta.env.VITE_TWIKOO_URL
      if (fromEnv) return resolveTwikooTarget(fromEnv)
      if (typeof window === 'undefined') return PROD_TWIKOO_URL
      return resolveTwikooTarget(SAME_ORIGIN_TWIKOO_PROXY)
    },

    htmlToText(html) {
      const s = (html || '').toString()
      if (!s) return ''
      if (typeof document === 'undefined') return s
      const el = document.createElement('div')
      el.innerHTML = s
      return (el.innerText || el.textContent || '').trim()
    },

    formatTime(created) {
      const ts = typeof created === 'number' ? created : Number(created)
      if (!ts || Number.isNaN(ts)) return ''
      const d = new Date(ts)
      if (Number.isNaN(d.getTime())) return ''
      return d.toLocaleString('zh-CN', { hour12: false })
    },

    mapCommentPathToPagePath(raw) {
      const s = (raw || '').toString()
      if (!s) return s
      // 友链页：Twikoo 历史评论聚合在 /link，但实际页面是 /links
      if (s === '/link') return '/links'
      const momentMatch = s.match(/^\/moments\/(\d+)$/)
      if (momentMatch) return `/moments#moment-${momentMatch[1]}`
      return s
    },

    formatCommentPathLabel(raw) {
      const s = (raw || '').toString()
      if (!s) return '未知页面'
      if (s === '/link') return '/links'
      const momentMatch = s.match(/^\/moments\/(\d+)$/)
      if (momentMatch) return `朋友圈 / #${momentMatch[1]}`
      return this.mapCommentPathToPagePath(s)
    },

    twikooKeysForUrl(rawUrl) {
      const pagePath = this.mapCommentPathToPagePath(rawUrl)
      const origin = window.location.origin
      const raw = (rawUrl || '').toString()

      const keys = new Set()
      if (raw) keys.add(raw)
      if (pagePath && pagePath !== raw) keys.add(pagePath)
      const rawIsAbs = raw.startsWith('http://') || raw.startsWith('https://')
      const pagePathIsAbs = pagePath && (pagePath.startsWith('http://') || pagePath.startsWith('https://'))
      if (raw && !rawIsAbs) keys.add(`${origin}${raw}`)
      if (pagePath && pagePath !== raw && !pagePathIsAbs) keys.add(`${origin}${pagePath}`)

      return Array.from(keys).filter(Boolean)
    },

    async twikooCommentGet({ url, before }) {
      const payload = { event: 'COMMENT_GET', url }
      if (before) payload.before = before
      const envId = this.getTwikooEnvId()

      let r
      try {
        r = await fetch(envId, {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify(payload),
        })
      } catch (e) {
        const message = e && e.message ? e.message : String(e || '')
        if (/Failed to fetch|NetworkError|Load failed|fetch failed/i.test(message)) {
          throw new Error(`${TWIKOO_UNAVAILABLE_HINT} 当前目标：${formatTwikooTarget(envId)}`)
        }
        throw e instanceof Error ? e : new Error(message || `Twikoo 服务当前不可用。当前目标：${formatTwikooTarget(envId)}`)
      }

      if (!r.ok) {
        let detail = ''
        try {
          detail = await r.text()
        } catch (_) {}
        throw new Error(buildTwikooRequestErrorMessage(r.status, detail, envId))
      }
      return await r.json()
    },

    async fetchTopCommentsForUrl(rawUrl, limitPerUrl = 3) {
      const keys = this.twikooKeysForUrl(rawUrl)
      const dedup = new Map()

      for (const k of keys) {
        if (dedup.size >= limitPerUrl) break
        const res = await this.twikooCommentGet({ url: k })
        const data = Array.isArray(res?.data) ? res.data : []
        for (const c of data) {
          if (!c || !c.id) continue
          if (dedup.has(c.id)) continue

          const textRaw = c.commentText ?? c.comment ?? c.text ?? ''
          const text = this.htmlToText(textRaw)
          dedup.set(c.id, {
            id: c.id,
            nick: c.nick || '匿名',
            created: c.created,
            page: this.formatCommentPathLabel(rawUrl),
            text,
          })
        }
      }

      return Array.from(dedup.values()).slice(0, limitPerUrl)
    },

    async loadLatestComments() {
      this.loadingComments = true
      this.latestComments = []
      this.commentsError = ''
      try {
        const [postResp, momentResp] = await Promise.allSettled([
          getPostList({ page: 1, pageSize: 20, orderBy: 'update_time', orderType: 'desc' }),
          getMomentList({ page: 1, pageSize: 8, status: 1 }),
        ])
        const posts = postResp.status === 'fulfilled' ? (postResp.value?.rows || []) : []
        const moments = momentResp.status === 'fulfilled' ? (momentResp.value?.rows || []) : []

        // 为了避免拉全站所有页面，这里取“最新评论更可能出现的页面”：
        // 1) 留言页 /comments、友链页 /link
        // 2) 最近更新的若干篇文章（取前 8 篇）
        // 3) 最近发布的朋友圈动态（取前 8 条）
        const urlKeys = [
          '/comments',
          '/link',
          ...posts.slice(0, 8).map((p) => `/posts/${p.id}`),
          ...moments.slice(0, 8).map((item) => `/moments/${item.id}`),
        ]

        const out = []
        const concurrency = 3
        let idx = 0
        let unavailableError = ''

        const worker = async () => {
          while (idx < urlKeys.length) {
            const u = urlKeys[idx++]
            try {
              const items = await this.fetchTopCommentsForUrl(u, 3)
              out.push(...items)
            } catch (e) {
              const message = e && e.message ? e.message : String(e || '')
              if (isTwikooServiceUnavailableError(message)) {
                unavailableError = unavailableError || message
              }
              console.warn('拉取评论失败:', u, e)
            }
          }
        }

        const workers = Array.from({ length: Math.min(concurrency, urlKeys.length) }, () => worker())
        await Promise.all(workers)

        const dedup = new Map()
        for (const c of out) {
          if (!c?.id) continue
          dedup.set(c.id, c)
        }

        const list = Array.from(dedup.values())
          .sort((a, b) => (Number(b.created) || 0) - (Number(a.created) || 0))
          .slice(0, 10)
          .map((x) => ({
            ...x,
            timeText: this.formatTime(x.created),
            text: x.text || '',
          }))

        if (!list.length && unavailableError) {
          this.commentsError = unavailableError
          return
        }

        this.latestComments = list
      } catch (e) {
        console.error('加载最新评论失败:', e)
        const message = e && e.message ? e.message : '加载最新评论失败'
        if (isTwikooServiceUnavailableError(message)) {
          this.commentsError = message
          return
        }
        this.$message.error('加载最新评论失败')
      } finally {
        this.loadingComments = false
      }
    },
  },
}
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 14px;
}

.dashboard-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--admin-border-soft);
  background: var(--admin-panel);
  box-shadow: 0 12px 22px var(--admin-shadow);
}

.head-title {
  margin: 0;
  font-size: 20px;
}

.head-sub {
  color: var(--admin-text-muted);
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.6;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.days-select {
  width: 140px;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-height: 116px;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid var(--admin-border-soft);
  background: var(--admin-panel);
}

.metric-card :deep(.el-card__body) {
  padding: 16px;
}

.metric-label {
  color: var(--admin-text-muted);
  font-size: 12px;
  margin-bottom: 8px;
}

.metric-value {
  font-family: var(--admin-mono);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.5px;
  color: var(--admin-text);
}

.metric-extra {
  margin-top: 8px;
  color: var(--admin-text-muted);
  font-size: 12px;
}

.chart-card,
.comments-card {
  border-radius: 18px;
  border: 1px solid var(--admin-border-soft);
  background: var(--admin-panel);
}

.chart-card :deep(.el-card__header),
.comments-card :deep(.el-card__header) {
  padding: 16px 16px 0;
  border-bottom: none;
}

.chart-card :deep(.el-card__body),
.comments-card :deep(.el-card__body) {
  padding: 14px 16px 16px;
}

.chart {
  height: 320px;
  width: 100%;
}

.comments-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.comment-text {
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-tips {
  padding: 12px 0 0;
  color: var(--admin-text-soft);
  font-size: 13px;
  text-align: center;
}

.loading-wrap {
  padding: 6px 0;
}

.comments-error {
  padding: 12px 0 0;
  color: #c65a4c;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .dashboard-head,
  .head-right {
    flex-direction: column;
    align-items: stretch;
  }

  .metric-row {
    grid-template-columns: 1fr;
  }

  .chart {
    height: 260px;
  }
}

/* ===== 重设计：扁平统计卡 + 面板（对齐 mockup） ===== */
.dashboard { gap: 16px; }
.dash-controls { display: flex; align-items: center; gap: 8px; }
.dash-controls__hint { color: var(--admin-text-soft); font-size: 12.5px; }
.dash-controls .spacer { flex: 1; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat { background: var(--admin-panel); border: 1px solid var(--admin-border); border-radius: 10px; padding: 15px 16px; }
.stat__label { font-size: 12px; color: var(--admin-text-muted); }
.stat__value { font-family: var(--admin-mono); font-size: 28px; font-weight: 600; letter-spacing: -0.5px; color: var(--admin-text); margin-top: 10px; line-height: 1.1; }
.stat__hint { font-family: var(--admin-mono); font-size: 11.5px; color: var(--admin-text-soft); margin-top: 6px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel { background: var(--admin-panel); border: 1px solid var(--admin-border); border-radius: 10px; }
.panel__head { display: flex; align-items: center; justify-content: space-between; padding: 13px 16px; border-bottom: 1px solid var(--admin-border); }
.panel__head h2 { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 13.5px; font-weight: 600; }
.panel__head h2::before { content: ""; width: 3px; height: 13px; background: var(--admin-accent); border-radius: 2px; }
.panel__body { padding: 16px; }
@media (max-width: 900px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } .chart-grid { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .stat-grid { grid-template-columns: 1fr; } }
</style>
