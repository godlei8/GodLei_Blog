<template>
  <div class="comments">
    <div class="comments-inner">
      <div class="comments-header">
        <div class="comments-tagline">SAY&nbsp;HELLO</div>
        <div class="comments-title-gradient">COMMENTS</div>
      </div>

      <p class="comments-subtitle">
        这里是当前页面的留言区，欢迎留下你的想法。
      </p>

      <div class="comment-card">
        <div class="card-header">
          <h2 class="card-title">留言区</h2>
          <span class="meta-muted">提示：这里发表的是本页（/comments）评论</span>
        </div>
        <div id="tcomment" ref="twikooEmbed" class="twikoo-wrap"></div>
        <p v-if="twikooEmbedError" class="twikoo-embed-error">{{ twikooEmbedError }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getTwikooEnvId, loadTwikoo, normalizeTwikooError } from '@/utils/twikoo'

export default {
  name: 'Comments',
  data() {
    return {
      twikooEmbedError: ''
    }
  },
  computed: {
    twikooEnvId() {
      return getTwikooEnvId()
    }
  },
  mounted() {
    this.initTwikooEmbed()
  },
  methods: {
    async initTwikooEmbed() {
      this.twikooEmbedError = ''
      await this.$nextTick()
      const el = this.$refs.twikooEmbed
      if (!el) {
        this.twikooEmbedError = '留言区容器未就绪，请刷新重试'
        return
      }
      try {
        const twikoo = await loadTwikoo()
        await Promise.resolve(
          twikoo.init({
            envId: this.twikooEnvId,
            el,
            path: '/comments'
          })
        )
      } catch (error) {
        const err = normalizeTwikooError(error, this.twikooEnvId)
        const msg = err && err.message ? err.message : String(err)
        console.error('Twikoo 留言区初始化失败', err)
        this.twikooEmbedError = msg
      }
    }
  }
}
</script>

<style scoped>
.comments {
  width: 100%;
  box-sizing: border-box;
  color: white;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  min-height: calc(100vh - 68px);
  padding: 20px;
}

.comments-inner {
  width: 100%;
  max-width: 900px;
}

.comments-header {
  font-family: 'Bebas Neue', Arial, sans-serif;
  margin-bottom: 8px;
}

.comments-tagline {
  font-size: 20px;
  letter-spacing: 4px;
  color: #cccccc;
}

.comments-title-gradient {
  font-size: 46px;
  letter-spacing: 6px;
  background-image: var(--theme-accent-title-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.comments-subtitle {
  margin-bottom: 18px;
  color: #cccccc;
  line-height: 1.7;
}

.sub-link {
  color: var(--theme-accent-text);
  text-decoration: none;
}

.sub-link:hover {
  text-decoration: underline;
}

.comment-card {
  margin-top: 20px;
  border-radius: 12px;
  padding: 18px;
  width: 100%;
  background: rgba(0, 0, 0, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.35);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.meta-muted {
  font-size: 12px;
  color: #c8c8c8;
}

#tcomment {
  color: white;
  min-height: 120px;
}

.twikoo-embed-error {
  margin-top: 10px;
  font-size: 13px;
  color: #ffb4b4;
  line-height: 1.5;
}

.twikoo-wrap :deep(.tk-comments) {
  color: rgba(255, 255, 255, 0.88);
}

.twikoo-wrap :deep(.tk-submit) {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  padding: 14px;
}

.twikoo-wrap :deep(.tk-submit .tk-row) {
  flex-wrap: wrap;
  gap: 10px;
}

.twikoo-wrap :deep(.tk-preview) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.twikoo-wrap :deep(.tk-avatar),
.twikoo-wrap :deep(.tk-avatar img) {
  border-radius: 50%;
}

.twikoo-wrap :deep(textarea),
.twikoo-wrap :deep(input),
.twikoo-wrap :deep(.tk-input) {
  color: #fff;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 10px;
  outline: none;
}

.twikoo-wrap :deep(textarea:focus),
.twikoo-wrap :deep(input:focus),
.twikoo-wrap :deep(.tk-input:focus) {
  border: 1px solid var(--theme-accent-border-strong);
  box-shadow: 0 0 0 3px var(--theme-accent-glow-soft);
}

.twikoo-wrap :deep(.tk-row.actions),
.twikoo-wrap :deep(.tk-row.actions > *) {
  color: rgba(255, 255, 255, 0.85);
}

.twikoo-wrap :deep(.tk-send),
.twikoo-wrap :deep(button),
.twikoo-wrap :deep(.tk-button) {
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: var(--theme-accent-panel-button);
  color: #fff;
  transition: transform 0.15s ease, border 0.15s ease, box-shadow 0.15s ease;
}

.twikoo-wrap :deep(.tk-send:hover),
.twikoo-wrap :deep(button:hover),
.twikoo-wrap :deep(.tk-button:hover) {
  transform: translateY(-1px);
  border: 1px solid var(--theme-accent-border);
  box-shadow: 0 6px 14px var(--theme-accent-shadow);
}

.twikoo-wrap :deep(.tk-comment) {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
  padding: 12px;
}

.twikoo-wrap :deep(.tk-content) {
  color: rgba(255, 255, 255, 0.86);
}

.twikoo-wrap :deep(.tk-nick) {
  color: #fff;
  font-weight: 700;
}

.twikoo-wrap :deep(.tk-time) {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
}

.twikoo-wrap :deep(.tk-action) {
  color: var(--theme-accent-text);
}

.twikoo-wrap :deep(.tk-action:hover) {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .comments {
    padding: 16px;
  }

  .comments-title-gradient {
    font-size: 38px;
  }

  .comment-card {
    padding: 16px 14px;
  }
}

@media (max-width: 520px) {
  .comments-tagline {
    font-size: 18px;
    letter-spacing: 3px;
  }

  .comments-title-gradient {
    font-size: 34px;
    letter-spacing: 4px;
  }

  .comments-subtitle {
    margin-bottom: 14px;
    font-size: 13px;
  }

  .card-header {
    align-items: flex-start;
  }

  .twikoo-wrap :deep(.tk-submit) {
    padding: 12px;
  }

  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-lg),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-md),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-sm),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-xs) {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
  }

  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col + .tk-col) {
    margin-left: 0;
  }

  .twikoo-wrap :deep(.tk-send),
  .twikoo-wrap :deep(.tk-button) {
    width: 100%;
  }
}

@media (max-width: 360px) {
  .comments {
    padding: 12px;
  }

  .comment-card {
    padding: 14px 12px;
  }

  .comments-title-gradient {
    font-size: 30px;
    letter-spacing: 3px;
  }
}
</style>
              this.partialErrors.push({ url, message: msg })
              nextMap.set(url, { url, items: [], count: 0, _error: msg })
            }
            this.loadedUrls += 1
          }
        }
        const workers = Array.from({ length: Math.min(concurrency, urls.length) }, () =>
          worker()
        )
        await Promise.all(workers)

        this.siteCommentMap = nextMap
        if (this.partialErrors.length) {
          const sample = this.partialErrors
            .slice(0, 3)
            .map((x) => `${x.url}（${x.message}）`)
            .join('；')
          this.loadError = `部分页面抓取失败：${this.partialErrors.length}/${this.totalUrls}${sample ? `，例如：${sample}` : ''}`
        }
      } catch (e) {
        this.loadError = e && e.message ? e.message : '未知错误'
        this.siteCommentMap = new Map()
      } finally {
        this.loadingAll = false
      }
    },

    async loadAllForUrl(url) {
      const fetchAllByKey = async (twikooUrlKey) => {
        const allTop = []
        let before = undefined
        let more = true
        let guard = 0
        let totalCount = 0

        while (more && guard < 10000) {
          guard += 1
          const res = await this.twikooCommentGet({ url: twikooUrlKey, before })
          const data = Array.isArray(res && res.data) ? res.data : []
          more = !!(res && res.more)
          totalCount = typeof res.count === 'number' ? res.count : totalCount
          allTop.push(...data)

          if (!more) break

          const createdList = []
          const collectCreated = (nodes) => {
            ;(nodes || []).forEach((n) => {
              if (n && (typeof n.created === 'number' || typeof n.created === 'string')) {
                const t = Number(n.created)
                if (!Number.isNaN(t) && t) createdList.push(t)
              }
              if (n && Array.isArray(n.replies) && n.replies.length) collectCreated(n.replies)
            })
          }
          collectCreated(data)
          const minCreated = createdList.length ? Math.min(...createdList) : 0
          if (!minCreated || (before && minCreated >= before)) {
            more = false
            break
          }
          before = minCreated
        }

        return { items: allTop, count: totalCount }
      }

      const keys = this.twikooKeysForUrl(url)

      const mergedTop = []
      let mergedCount = 0
      for (const k of keys) {
        const { items, count } = await fetchAllByKey(k)
        mergedTop.push(...(items || []))
        if (typeof count === 'number' && count > mergedCount) mergedCount = count
      }

      const seen = new Set()
      const dedupTree = (nodes) => {
        const out = []
        ;(nodes || []).forEach((n) => {
          if (!n || !n.id) return
          if (seen.has(n.id)) return
          seen.add(n.id)
          const replies = Array.isArray(n.replies) ? dedupTree(n.replies) : []
          out.push(this.normalizeCommentNode({ ...n, url, replies }))
        })
        return out
      }
      const items = dedupTree(mergedTop)
      const count = mergedCount || this.countTree(items)
      return { url, items, count }
    },

    countTree(nodes) {
      let c = 0
      const walk = (arr) => {
        ;(arr || []).forEach((n) => {
          c += 1
          if (n && Array.isArray(n.replies) && n.replies.length) walk(n.replies)
        })
      }
      walk(nodes)
      return c
    },

    async twikooCommentGet({ url, before }, retryCount = 3) {
      const payload = { event: 'COMMENT_GET', url }
      if (before) payload.before = before

      for (let attempt = 0; attempt < retryCount; attempt++) {
        try {
          const r = await fetch(this.twikooEnvId, {
            method: 'POST',
            headers: { 'content-type': 'application/json' },
            body: JSON.stringify(payload)
          })
          if (!r.ok) {
            if (r.status === 429 && attempt < retryCount - 1) {
              const waitTime = Math.pow(2, attempt) * 1000
              await this.delay(waitTime)
              continue
            }
            let detail = ''
            try {
              detail = await r.text()
            } catch (_) {
              // ignore
            }
            throw new Error(`Twikoo 请求失败：${r.status}${detail ? ` ${detail}` : ''}`)
          }
          return await r.json()
        } catch (e) {
          if (
            attempt < retryCount - 1 &&
            (e.message?.includes('fetch') || e.message?.includes('network'))
          ) {
            const waitTime = Math.pow(2, attempt) * 1000
            await this.delay(waitTime)
            continue
          }
          throw e
        }
      }
      throw new Error('Twikoo 请求失败：超过最大重试次数')
    },

    displayPath(u) {
      const s = this.mapCommentPathToPagePath((u || '').toString())
      if (!s) return '未知页面'
      try {
        const parsed = new URL(s, window.location.origin)
        return parsed.pathname || s
      } catch (_) {
        return s
      }
    },

    formatTime(created, relativeTime) {
      if (relativeTime) return relativeTime
      const ts = typeof created === 'number' ? created : Number(created)
      if (!ts || Number.isNaN(ts)) return ''
      const d = new Date(ts)
      if (Number.isNaN(d.getTime())) return ''
      return d.toLocaleString('zh-CN', { hour12: false })
    },

    fallbackAvatar(item) {
      const md5 = item && item.mailMd5 ? String(item.mailMd5) : ''
      if (!md5) return 'https://www.weavatar.com/avatar/?d=mp&s=96'
      return `https://www.weavatar.com/avatar/${md5}?d=mp&s=96`
    }
  }
</script>

<style scoped>
.comments {
  width: 100%;
  box-sizing: border-box;
  color: white;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  min-height: calc(100vh - 68px);
  padding: 20px;
  --indent-step: 16px;
}

.comments-inner {
  width: 100%;
  max-width: 1100px;
}

.comments-header {
  font-family: 'Bebas Neue', Arial, sans-serif;
  margin-bottom: 8px;
}

.comments-tagline {
  font-size: 20px;
  letter-spacing: 4px;
  color: #cccccc;
}

.comments-title-gradient {
  font-size: 46px;
  letter-spacing: 6px;
  background-image: var(--theme-accent-title-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.comments-subtitle {
  margin-bottom: 18px;
  color: #cccccc;
}

.sub-link {
  color: var(--theme-accent-text);
  text-decoration: none;
}

.sub-link:hover {
  text-decoration: underline;
}

.highlight {
  color: var(--theme-accent-text);
  font-weight: 600;
}

.comment-card {
  margin-top: 20px;
  border-radius: 12px;
  padding: 18px 18px;
  width: 100%;
  background: rgba(0, 0, 0, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.35);
}

.comment-card--all {
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
  background: var(--theme-accent-panel);
  border: 1px solid var(--theme-accent-border-soft);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--theme-accent-text-strong);
}

.seg-btn {
  height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  cursor: pointer;
  transition: transform 0.15s ease, border 0.15s ease, box-shadow 0.15s ease,
    background 0.15s ease;
}

.seg-btn:hover {
  transform: translateY(-1px);
  border: 1px solid var(--theme-accent-border);
  box-shadow: 0 6px 14px var(--theme-accent-shadow);
}

.seg-btn:focus-visible {
  outline: none;
  border: 1px solid var(--theme-accent-border-strong);
  box-shadow: 0 0 0 3px var(--theme-accent-glow-soft);
}

.seg-btn--active {
  border: 1px solid var(--theme-accent-border-strong);
  background: var(--theme-accent-surface);
}

.refresh-btn {
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid var(--theme-accent-border-soft);
  background: var(--theme-accent-panel-button);
  color: #fff;
  cursor: pointer;
  transition: transform 0.15s ease, border 0.15s ease, box-shadow 0.15s ease,
    background 0.15s ease;
}

.refresh-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  border: 1px solid var(--theme-accent-border-strong);
  box-shadow: 0 6px 14px var(--theme-accent-shadow);
}

.refresh-btn:focus-visible {
  outline: none;
  border: 1px solid var(--theme-accent-border-strong);
  box-shadow: 0 0 0 3px var(--theme-accent-glow-soft);
}

.refresh-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.recent-meta {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--theme-accent-surface-soft);
  border: 1px solid var(--theme-accent-border);
  color: var(--theme-accent-text-soft);
  font-size: 12px;
}

.meta-muted {
  font-size: 12px;
  color: #c8c8c8;
}

.meta-error {
  font-size: 12px;
  color: #ffb4b4;
}

.empty-state {
  margin-top: 18px;
  padding: 18px 0 4px;
  text-align: center;
  color: #cfcfcf;
}

.recent-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  border: 1px solid var(--theme-accent-border-soft);
  background: var(--theme-accent-panel-soft);
  transition: transform 0.15s ease, border 0.15s ease, box-shadow 0.15s ease;
}

.recent-item:hover {
  transform: translateY(-1px);
  border: 1px solid var(--theme-accent-border);
  box-shadow: 0 6px 14px var(--theme-accent-shadow);
}

.recent-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.25);
  flex: 0 0 auto;
}

.recent-body {
  overflow: hidden;
  flex: 1;
}

.recent-top {
  display: flex;
  gap: 10px;
  align-items: baseline;
  justify-content: space-between;
}

.recent-nick {
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}

.recent-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
  flex: 0 0 auto;
}

.recent-url {
  margin-top: 2px;
  font-size: 12px;
  color: var(--theme-accent-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-tag {
  margin-left: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.recent-text {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.4;
  display: -webkit-box;
  line-clamp: 3;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tree-groups {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tree-group {
  border-radius: 10px;
  border: 1px solid var(--theme-accent-border-soft);
  background: var(--theme-accent-panel-soft);
  padding: 12px;
}

.group-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
}

.group-link {
  color: var(--theme-accent-text);
  text-decoration: none;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-link:hover {
  text-decoration: underline;
}

.group-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
}

.group-empty {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.tree-list {
  margin-top: 10px;
}

.node {
  padding: 8px 0;
  margin-left: min(calc(var(--depth, 0) * var(--indent-step, 16px)), 64px);
}

.node-row {
  display: flex;
  gap: 10px;
}

.node-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.22);
  flex: 0 0 auto;
}

.node-body {
  flex: 1;
  overflow: hidden;
}

.node-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: baseline;
}

.node-nick {
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}

.node-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
}

.node-meta {
  margin-top: 2px;
}

.node-link {
  font-size: 12px;
  color: var(--theme-accent-text);
  text-decoration: none;
  display: block;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.node-link:hover {
  text-decoration: underline;
}

.node-text {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}

.node-children {
  margin-top: 6px;
  padding-left: 10px;
  border-left: 2px solid var(--theme-accent-border);
}

#tcomment {
  color: white;
  min-height: 120px;
}

.twikoo-embed-error {
  margin-top: 10px;
  font-size: 13px;
  color: #ffb4b4;
  line-height: 1.5;
}

/* Twikoo：用 :deep 覆盖内部样式，贴合站点黑金/绿色调 */
.twikoo-wrap :deep(.tk-comments) {
  color: rgba(255, 255, 255, 0.88);
}

.twikoo-wrap :deep(.tk-submit) {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  padding: 14px 14px;
}

.twikoo-wrap :deep(.tk-submit .tk-row) {
  flex-wrap: wrap;
  gap: 10px;
}

.twikoo-wrap :deep(.tk-preview) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.twikoo-wrap :deep(.tk-avatar),
.twikoo-wrap :deep(.tk-avatar img) {
  border-radius: 50%;
}

.twikoo-wrap :deep(textarea),
.twikoo-wrap :deep(input),
.twikoo-wrap :deep(.tk-input) {
  color: #fff;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 10px;
  outline: none;
}

.twikoo-wrap :deep(textarea:focus),
.twikoo-wrap :deep(input:focus),
.twikoo-wrap :deep(.tk-input:focus) {
  border: 1px solid var(--theme-accent-border-strong);
  box-shadow: 0 0 0 3px var(--theme-accent-glow-soft);
}

.twikoo-wrap :deep(.tk-row.actions),
.twikoo-wrap :deep(.tk-row.actions > *) {
  color: rgba(255, 255, 255, 0.85);
}

.twikoo-wrap :deep(.tk-send),
.twikoo-wrap :deep(button),
.twikoo-wrap :deep(.tk-button) {
  border-radius: 10px;
  border: 1px solid var(--theme-accent-border-soft);
  background: var(--theme-accent-panel-button);
  color: #fff;
  transition: transform 0.15s ease, border 0.15s ease, box-shadow 0.15s ease;
}

.twikoo-wrap :deep(.tk-send:hover),
.twikoo-wrap :deep(button:hover),
.twikoo-wrap :deep(.tk-button:hover) {
  transform: translateY(-1px);
  border: 1px solid var(--theme-accent-border-strong);
  box-shadow: 0 6px 14px var(--theme-accent-shadow);
}

.twikoo-wrap :deep(.tk-comment) {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
  padding: 12px 12px;
}

.twikoo-wrap :deep(.tk-content) {
  color: rgba(255, 255, 255, 0.86);
}

.twikoo-wrap :deep(.tk-nick) {
  color: #fff;
  font-weight: 700;
}

.twikoo-wrap :deep(.tk-time) {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
}

.twikoo-wrap :deep(.tk-action) {
  color: var(--theme-accent-text);
}

.twikoo-wrap :deep(.tk-action:hover) {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .comments {
    padding: 16px;
    --indent-step: 14px;
  }

  .comments-title-gradient {
    font-size: 38px;
  }

  .comment-card {
    padding: 16px 14px;
  }
}

@media (max-width: 520px) {
  .comments-tagline {
    font-size: 18px;
    letter-spacing: 3px;
  }

  .comments-title-gradient {
    font-size: 34px;
    letter-spacing: 4px;
  }

  .comments-subtitle {
    margin-bottom: 14px;
    font-size: 13px;
    line-height: 1.6;
  }

  .card-header {
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .seg-btn {
    flex: 1 1 auto;
  }

  .refresh-btn {
    flex: 1 1 100%;
  }

  .recent-item {
    padding: 12px 10px;
    gap: 10px;
  }

  .recent-avatar {
    width: 34px;
    height: 34px;
  }

  .recent-top {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }

  .recent-nick {
    max-width: 100%;
  }

  .recent-url {
    white-space: normal;
    overflow: visible;
    text-overflow: initial;
    word-break: break-word;
  }

  .reply-tag {
    display: inline-block;
    margin-left: 0;
    margin-top: 4px;
  }

  .node-top {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }

  .node-nick {
    max-width: 100%;
  }

  .group-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .group-link {
    white-space: normal;
    word-break: break-word;
  }

  .node-children {
    padding-left: 8px;
  }

  .comments {
    --indent-step: 12px;
  }

  .node {
    margin-left: min(calc(var(--depth, 0) * var(--indent-step, 12px)), 36px);
  }

  .twikoo-wrap :deep(.tk-submit) {
    padding: 12px 12px;
  }

  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-lg),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-md),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-sm),
  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col-xs) {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
  }

  .twikoo-wrap :deep(.tk-submit .tk-row .tk-col + .tk-col) {
    margin-left: 0;
  }

  .twikoo-wrap :deep(.tk-send),
  .twikoo-wrap :deep(.tk-button) {
    width: 100%;
  }
}

@media (max-width: 360px) {
  .comments {
    padding: 12px;
  }

  .comment-card {
    padding: 14px 12px;
  }

  .comments-title-gradient {
    font-size: 30px;
    letter-spacing: 3px;
  }
}
</style>
