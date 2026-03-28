<template>
  <div class="comments">
    <div class="comments-inner">
      <div class="comments-header">
        <div class="comments-tagline">SAY&nbsp;HELLO</div>
        <div class="comments-title-gradient">COMMENTS</div>
      </div>

      <p class="comments-subtitle">
        这里是当前页面的留言区，欢迎留下你的想法。
        旧站留言页请看：
        <a class="sub-link" href="https://blog.godlei.cn/comments" target="_blank" rel="noopener">旧站留言页</a>
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
