<template>
  <div class="logs-manage-page page-stack">
    <section class="page-card">
      <div class="page-toolbar page-toolbar--split">
        <div class="page-toolbar__group">
          <el-input
            v-model="keyword"
            placeholder="输入版本号，例如 v1.3.0"
            clearable
            class="toolbar-input"
            @keyup.enter="fetchList"
          />
        </div>

        <div class="page-toolbar__group">
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button type="success" @click="openAddDialog">新增版本</el-button>
          <el-button type="info" @click="goToContentView">查看日志内容</el-button>
        </div>
      </div>

      <div class="page-stat-grid">
        <article class="page-stat-card">
          <span class="page-stat-card__label">当前生效版本</span>
          <strong class="page-stat-card__value">{{ currentVersionText }}</strong>
          <span class="page-stat-card__hint">{{ currentVersionDate }}</span>
        </article>

        <article class="page-stat-card">
          <span class="page-stat-card__label">版本总数</span>
          <strong class="page-stat-card__value">{{ tableData.length }}</strong>
          <span class="page-stat-card__hint">当前已收录 {{ tableData.length }} 个日志版本。</span>
        </article>

        <article class="page-stat-card">
          <span class="page-stat-card__label">筛选状态</span>
          <strong class="page-stat-card__value logs-stat-text">{{ keywordState }}</strong>
          <span class="page-stat-card__hint">支持按版本号快速检索。</span>
        </article>
      </div>
    </section>

    <section class="page-card">
      <div class="section-head">
        <div class="section-head__copy">
          <h3 class="section-head__title">版本列表</h3>
          <p class="section-head__desc">可以切换当前版本、编辑变更内容，或删除旧版本记录。</p>
        </div>
      </div>

      <div class="table-shell">
        <div class="table-wrap">
          <el-table :data="tableData" stripe style="width: 100%" empty-text="暂无日志版本">
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="date" label="日期" width="140" />
            <el-table-column prop="version" label="版本号" min-width="180" />
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.current" type="success">当前</el-tag>
                <el-tag v-else type="info">历史</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="scope">
                <div class="row-actions">
                  <el-button
                    v-if="!scope.row.current"
                    size="small"
                    type="primary"
                    @click="setCurrent(scope.row)"
                  >
                    设为当前
                  </el-button>
                  <el-button size="small" type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="removeLogVersion(scope.row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </section>

    <el-dialog :title="isEdit ? '编辑日志版本' : '新增日志版本'" v-model="dialogVisible" width="760px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="版本号">
          <el-input v-model="form.version" placeholder="例如 v1.3.0" />
        </el-form-item>

        <el-form-item label="版本日期">
          <el-date-picker
            v-model="form.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="变更内容">
          <el-input
            v-model="form.changesText"
            placeholder="一行一条变更内容"
            type="textarea"
            :rows="6"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  addLogVersion,
  deleteLogVersion,
  getLogList,
  setCurrentLogVersion,
  updateLogVersion
} from '@/api'

export default {
  data() {
    return {
      keyword: '',
      tableData: [],
      currentVersion: null,

      dialogVisible: false,
      isEdit: false,
      form: {
        id: null,
        version: '',
        date: '',
        changesText: ''
      }
    }
  },
  computed: {
    currentVersionText() {
      return this.currentVersion?.version || '暂无'
    },
    currentVersionDate() {
      return this.currentVersion?.date ? `发布日期 ${this.currentVersion.date}` : '还没有设置当前生效版本'
    },
    keywordState() {
      return this.keyword?.trim() ? '筛选中' : '全部版本'
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      try {
        const data = await getLogList({ keyword: this.keyword || undefined })
        this.tableData = Array.isArray(data) ? data : []
        this.currentVersion = this.tableData.find((i) => i.current) || null
      } catch (error) {
        console.error('获取日志列表失败:', error)
        this.$message.error('获取日志列表失败')
      }
    },
    openAddDialog() {
      this.isEdit = false
      this.form = { id: null, version: '', date: '', changesText: '' }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      this.form = {
        id: row.id,
        version: row.version || '',
        date: row.date || '',
        changesText: Array.isArray(row.changes) ? row.changes.join('\n') : ''
      }
      this.dialogVisible = true
    },
    normalizeChanges() {
      return (this.form.changesText || '')
        .split('\n')
        .map((s) => s.trim())
        .filter((s) => s)
    },
    async submitForm() {
      if (!this.form.version || !this.form.version.trim()) {
        this.$message.warning('请输入版本号')
        return
      }
      if (!this.form.date) {
        this.$message.warning('请选择版本日期')
        return
      }
      const changes = this.normalizeChanges()
      if (!changes.length) {
        this.$message.warning('请至少填写一条变更内容')
        return
      }

      const payload = {
        id: this.form.id,
        version: this.form.version.trim(),
        date: this.form.date,
        changes
      }

      try {
        if (this.isEdit) {
          await updateLogVersion(payload)
          this.$message.success('日志版本更新成功')
        } else {
          await addLogVersion(payload)
          this.$message.success('日志版本新增成功')
        }
        this.dialogVisible = false
        await this.fetchList()
      } catch (error) {
        console.error('保存日志版本失败:', error)
        this.$message.error('保存日志版本失败')
      }
    },
    async setCurrent(row) {
      try {
        await setCurrentLogVersion({ id: row.id })
        this.$message.success('已切换当前版本')
        await this.fetchList()
      } catch (error) {
        console.error('切换当前版本失败:', error)
        this.$message.error('切换当前版本失败')
      }
    },
    removeLogVersion(row) {
      this.$confirm(`确定删除版本 "${row.version}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          await deleteLogVersion({ id: row.id })
          this.$message.success('删除成功')
          await this.fetchList()
        })
        .catch(() => {})
    },
    goToContentView() {
      this.$router.push('/logs/content')
    }
  }
}
</script>

<style scoped>
.logs-stat-text {
  font-size: 20px;
}
</style>
