<template>
  <div class="logs-view-page page-card">
    <div class="page-toolbar">
      <el-button type="primary" @click="fetchList">刷新</el-button>
    </div>

    <div class="current-info">
      <span class="current-label">当前生效版本：</span>
      <el-tag v-if="currentVersion" type="success">
        {{ currentVersion.version }} ({{ currentVersion.date }})
      </el-tag>
      <el-tag v-else type="info">暂无</el-tag>
    </div>

    <div class="timeline">
      <div v-for="log in logs" :key="log.id" class="log-item">
        <div class="date">{{ log.date }}</div>
        <div class="content" :class="{ current: log.current }">
          <h2>
            {{ log.version }}
            <el-tag v-if="log.current" size="small" type="success" style="margin-left: 8px;">当前</el-tag>
          </h2>
          <ul>
            <li v-for="(change, i) in (log.changes || [])" :key="i">{{ change }}</li>
          </ul>
        </div>
      </div>
      <el-empty v-if="!logs.length" description="暂无日志版本" />
    </div>
  </div>
</template>

<script>
import { getLogList } from '@/api'

export default {
  name: 'LogsView',
  data() {
    return {
      logs: [],
      currentVersion: null
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      try {
        const data = await getLogList({})
        this.logs = Array.isArray(data) ? data : []
        this.currentVersion = this.logs.find((i) => i.current) || null
      } catch (error) {
        console.error('获取日志内容失败:', error)
        this.$message.error('获取日志内容失败')
      }
    }
  }
}
</script>

<style scoped>
.logs-view-page {
  padding: 16px;
}

.current-info {
  margin: 12px 0;
  padding: 10px 12px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(247, 239, 228, 0.98));
  border: 1px solid var(--admin-border);
  border-radius: 14px;
}

.current-label {
  font-weight: 600;
  margin-right: 8px;
  color: var(--admin-text);
}

.timeline {
  margin-top: 14px;
  position: relative;
}

.timeline:before {
  content: '';
  position: absolute;
  left: 140px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, rgba(199, 119, 66, 0.12), rgba(199, 119, 66, 0.56), rgba(199, 119, 66, 0.12));
}

.log-item {
  position: relative;
  display: flex;
  gap: 18px;
  margin-bottom: 18px;
  padding-left: 0;
}

.date {
  width: 130px;
  text-align: right;
  color: var(--admin-gold-soft);
  font-weight: 600;
  padding-right: 10px;
  flex: none;
}

.content {
  flex: 1;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(247, 239, 228, 0.96));
  border: 1px solid var(--admin-border);
  border-radius: 14px;
  padding: 12px 14px;
  box-shadow: 0 14px 28px rgba(143, 102, 62, 0.12);
}

.content.current {
  border-color: var(--admin-border-strong);
  background: linear-gradient(145deg, rgba(255, 248, 239, 0.98), rgba(239, 223, 202, 0.98), rgba(255, 255, 255, 0.96));
}

.content h2 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 700;
}

.content ul {
  margin: 0;
  padding-left: 18px;
}

.content li {
  margin: 6px 0;
  color: var(--admin-text);
}
</style>

