<template>
  <div class="article-list page-card">
    <div class="page-toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入文章标题关键词"
        class="toolbar-input"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button type="success" @click="goToAddArticle">添加文章</el-button>
    </div>

    <div class="table-wrap">
      <el-table :data="articles" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" width="500" />
        <el-table-column prop="cover" label="封面" width="300">
          <template #default="scope">
            <img v-if="scope.row.cover" :src="resolveCover(scope.row.cover)" alt="封面" class="cover-img" />
            <span v-else>无封面</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column prop="updateTime" label="更新时间" width="150" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <div class="row-actions">
              <el-button size="small" type="primary" @click="editArticle(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteArticle(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      @current-change="handlePageChange"
      class="page-pagination"
    />
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
.article-list {
  padding: 16px;
}

.cover-img {
  width: 100px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
}
</style>
