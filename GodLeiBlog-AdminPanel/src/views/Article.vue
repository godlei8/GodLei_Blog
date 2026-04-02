<template>
  <div class="article-list page-stack">
    <section class="page-card">
      <div class="page-toolbar page-toolbar--split">
        <div class="page-toolbar__group">
          <el-input
            v-model="searchKeyword"
            placeholder="输入文章标题关键词"
            class="toolbar-input"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="page-toolbar__group">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button type="success" @click="goToAddArticle">新增文章</el-button>
        </div>
      </div>

      <div class="page-stat-grid">
        <article class="page-stat-card">
          <span class="page-stat-card__label">文章总数</span>
          <strong class="page-stat-card__value">{{ total }}</strong>
          <span class="page-stat-card__hint">当前列表会随搜索关键词实时刷新。</span>
        </article>

        <article class="page-stat-card">
          <span class="page-stat-card__label">当前页码</span>
          <strong class="page-stat-card__value">{{ currentPage }}</strong>
          <span class="page-stat-card__hint">共 {{ totalPages }} 页</span>
        </article>

        <article class="page-stat-card">
          <span class="page-stat-card__label">筛选状态</span>
          <strong class="page-stat-card__value article-stat-text">{{ searchState }}</strong>
          <span class="page-stat-card__hint">当前展示 {{ currentRangeText }}</span>
        </article>
      </div>
    </section>

    <section class="page-card">
      <div class="section-head">
        <div class="section-head__copy">
          <h3 class="section-head__title">文章列表</h3>
          <p class="section-head__desc">支持按标题搜索、快速编辑和删除当前文章。</p>
        </div>
      </div>

      <div class="table-shell">
        <div class="table-wrap">
          <el-table :data="articles" stripe style="width: 100%" empty-text="暂无文章数据">
            <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
            <el-table-column label="封面" width="180">
              <template #default="scope">
                <div v-if="scope.row.cover" class="cover-thumb">
                  <img :src="resolveCover(scope.row.cover)" alt="cover" class="cover-thumb__image" />
                </div>
                <div v-else class="cover-thumb cover-thumb--empty">未设置</div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column prop="updateTime" label="更新时间" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <div class="row-actions">
                  <el-button size="small" type="primary" @click="editArticle(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteArticle(scope.row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
        class="page-pagination"
      />
    </section>
  </div>
</template>

<script>
import { deletePost, getPostList } from '@/api'

const normalizeMediaUrl = (url) => {
  const source = typeof url === 'string' ? url.trim() : ''
  if (!source) return ''
  if (source.startsWith('/api/uploads/')) return source
  if (source.startsWith('/uploads/')) return `/api${source}`
  if (source.startsWith('uploads/')) return `/api/${source}`
  return source
}

export default {
  name: 'Article',
  data() {
    return {
      articles: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      searchKeyword: ''
    }
  },
  computed: {
    totalPages() {
      return Math.max(Math.ceil((this.total || 0) / this.pageSize), 1)
    },
    searchState() {
      return this.searchKeyword?.trim() ? '筛选中' : '全部文章'
    },
    currentRangeText() {
      if (!this.total) return '0 条结果'
      const start = (this.currentPage - 1) * this.pageSize + 1
      const end = Math.min(this.currentPage * this.pageSize, this.total)
      return `${start}-${end} / ${this.total}`
    }
  },
  mounted() {
    this.fetchArticles()
  },
  methods: {
    resolveCover(url) {
      return normalizeMediaUrl(url)
    },
    async fetchArticles() {
      try {
        const data = await getPostList({
          page: this.currentPage,
          pageSize: this.pageSize,
          title: this.searchKeyword?.trim() || undefined
        })

        this.articles = data.rows
        this.total = data.total
      } catch (error) {
        console.error('获取文章列表失败:', error)
        this.$message.error('获取文章列表失败，请稍后再试')
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchArticles()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchArticles()
    },
    editArticle(row) {
      this.$router.push(`/edit-article/${row.id}`)
    },
    deleteArticle(row) {
      this.$confirm(`确定删除文章 "${row.title}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            await deletePost({ id: row.id })
            this.$message.success('删除成功')
            this.fetchArticles()
          } catch (error) {
            console.error('删除失败:', error)
            this.$message.error('删除失败，请稍后再试')
          }
        })
        .catch(() => {
          this.$message.info('已取消删除')
        })
    },
    goToAddArticle() {
      this.$router.push('/add-article')
    }
  }
}
</script>

<style scoped>
.cover-thumb {
  width: 132px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(214, 173, 92, 0.12);
  background: rgba(250, 246, 239, 0.92);
  color: var(--admin-text-soft);
  font-size: 12px;
}

.cover-thumb__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-stat-text {
  font-size: 20px;
}
</style>
