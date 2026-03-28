<template>
  <div class="add-article page-card">
    <h2>{{ isEdit ? '编辑文章' : '新增文章' }}</h2>

    <div class="form-section">
      <el-form :model="form" label-width="100px" class="article-form">
        <el-form-item label="文章标题">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔" />
        </el-form-item>

        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="多个分类层级可用英文逗号分隔" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入文章描述"
          />
        </el-form-item>

        <el-form-item label="封面图">
          <div class="form-input-row">
            <el-input v-model="form.cover" placeholder="请输入封面图片 URL" />
            <el-button :loading="uploadingCover" @click="uploadCoverImage">上传封面</el-button>
          </div>
        </el-form-item>

        <el-form-item label="短链接">
          <el-input
            v-model="form.abbrlink"
            :disabled="isEdit"
            placeholder="可选，不填则由后端自动生成"
          />
        </el-form-item>

        <el-form-item label="创建日期">
          <el-date-picker
            v-model="form.date"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            placeholder="请选择创建日期"
          />
        </el-form-item>

        <el-form-item label="更新日期">
          <el-date-picker
            v-model="form.updated"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            placeholder="请选择更新日期"
          />
        </el-form-item>
      </el-form>
    </div>

    <div class="editor-toolbar">
      <el-button :loading="uploadingContentImage" @click="uploadContentImage">
        上传图片并插入 Markdown
      </el-button>
    </div>

    <div class="editor-container">
      <div class="editor-left">
        <el-input
          ref="markdownEditor"
          v-model="markdownContent"
          type="textarea"
          :rows="30"
          placeholder="请输入 Markdown 内容..."
          @input="parseFrontMatter"
        />
      </div>
      <div class="editor-right" v-html="renderedHtml"></div>
    </div>

    <div class="submit-section">
      <el-button type="primary" @click="submitArticle">
        {{ isEdit ? '保存修改' : '提交文章' }}
      </el-button>
      <el-button @click="resetForm">重置</el-button>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import fm from 'front-matter'
import { addPost, getPostDetail, updatePost, uploadImage } from '@/api'

const createEmptyForm = () => ({
  title: '',
  tags: '',
  category: '',
  description: '',
  cover: '',
  abbrlink: '',
  date: '',
  updated: ''
})

export default {
  name: 'AddArticle',
  data() {
    return {
      loading: false,
      uploadingCover: false,
      uploadingContentImage: false,
      form: createEmptyForm(),
      parsedFrontMatter: {
        tags: [],
        categories: []
      },
      markdownContent: '',
      renderedHtml: ''
    }
  },
  computed: {
    isEdit() {
      return Boolean(this.$route.params && this.$route.params.id)
    },
    articleId() {
      return (this.$route.params && this.$route.params.id) || ''
    }
  },
  async created() {
    this.md = new MarkdownIt()
    if (this.isEdit) {
      await this.loadArticle()
    }
  },
  methods: {
    normalizeToArray(value) {
      if (value == null) return []
      if (Array.isArray(value)) return value.map((item) => String(item)).filter(Boolean)
      if (typeof value === 'string') {
        return value.split(',').map((item) => item.trim()).filter(Boolean)
      }
      return [String(value)]
    },
    formatToYmd(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return ''
      const yyyy = date.getFullYear()
      const mm = String(date.getMonth() + 1).padStart(2, '0')
      const dd = String(date.getDate()).padStart(2, '0')
      return `${yyyy}-${mm}-${dd}`
    },
    pickImage() {
      return new Promise((resolve) => {
        const input = document.createElement('input')
        input.type = 'file'
        input.accept = 'image/*'
        input.onchange = () => resolve(Array.from(input.files || []))
        input.click()
      })
    },
    async uploadCoverImage() {
      const files = await this.pickImage()
      if (!files.length) return

      this.uploadingCover = true
      try {
        const result = await uploadImage(files[0], 'post-cover')
        this.form.cover = result.url
        this.$message.success('封面上传成功')
      } catch (error) {
        console.error('上传封面失败:', error)
        this.$message.error('封面上传失败，请稍后重试')
      } finally {
        this.uploadingCover = false
      }
    },
    async uploadContentImage() {
      const files = await this.pickImage()
      if (!files.length) return

      this.uploadingContentImage = true
      try {
        const result = await uploadImage(files[0], 'post-content')
        const imageName = files[0].name || 'image'
        this.insertMarkdownAtCursor(`\n![${imageName}](${result.url})\n`)
        this.$message.success('图片已插入 Markdown')
      } catch (error) {
        console.error('上传文章图片失败:', error)
        this.$message.error('文章图片上传失败，请稍后重试')
      } finally {
        this.uploadingContentImage = false
      }
    },
    insertMarkdownAtCursor(content) {
      const editorRef = this.$refs.markdownEditor
      const textarea = editorRef && editorRef.textarea ? editorRef.textarea : null

      if (!textarea) {
        this.markdownContent = `${this.markdownContent}${content}`
        this.parseFrontMatter()
        return
      }

      const start = textarea.selectionStart ?? this.markdownContent.length
      const end = textarea.selectionEnd ?? this.markdownContent.length
      this.markdownContent = `${this.markdownContent.slice(0, start)}${content}${this.markdownContent.slice(end)}`

      this.$nextTick(() => {
        const nextCursor = start + content.length
        textarea.focus()
        textarea.setSelectionRange(nextCursor, nextCursor)
        this.parseFrontMatter()
      })
    },
    async loadArticle() {
      this.loading = true
      try {
        const post = await getPostDetail({ id: this.articleId })
        if (!post) {
          this.$message.error('文章不存在或已被删除')
          this.$router.push('/article')
          return
        }
        this.form.title = post.title || ''
        this.form.description = post.description || ''
        this.form.cover = post.cover || ''
        this.form.abbrlink = post.id || this.articleId
        this.form.date = this.formatToYmd(post.createTime)
        this.form.updated = this.formatToYmd(post.updateTime)
        this.markdownContent = post.content || ''

        this.parseFrontMatter()
        if (!this.renderedHtml) {
          this.renderedHtml = this.md.render(this.markdownContent || '')
        }
      } catch (error) {
        console.error('获取文章详情失败:', error)
        this.$message.error('获取文章详情失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    parseFrontMatter() {
      try {
        const { attributes, body } = fm(this.markdownContent)
        this.form.title = attributes.title || this.form.title || ''

        const tagsArr = this.normalizeToArray(attributes.tags)
        const categoriesRaw = attributes.categories != null ? attributes.categories : attributes.category
        const categoriesArr = this.normalizeToArray(categoriesRaw)

        this.parsedFrontMatter.tags = tagsArr
        this.parsedFrontMatter.categories = categoriesArr

        if (tagsArr.length) {
          this.form.tags = tagsArr.join(',')
        }
        if (categoriesArr.length) {
          this.form.category = categoriesArr.join(',')
        }
        if (attributes.description != null) {
          this.form.description = String(attributes.description)
        }
        if (attributes.cover) {
          this.form.cover = attributes.cover
        }
        if (attributes.abbrlink) {
          this.form.abbrlink = attributes.abbrlink
        }

        this.form.date = this.parseDate(attributes.date) || this.form.date
        this.form.updated = this.parseDate(attributes.updated) || this.form.updated
        this.renderedHtml = this.md.render(body)
      } catch (error) {
        console.error('Front-Matter 解析失败:', error)
        this.renderedHtml = this.md.render(this.markdownContent || '')
      }
    },
    parseDate(value) {
      if (!value) return ''
      const raw = String(value).trim()
      if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
        return raw
      }
      const date = new Date(raw)
      if (Number.isNaN(date.getTime())) {
        return ''
      }
      return date.toISOString().split('T')[0]
    },
    async submitArticle() {
      if (!this.form.title || !this.markdownContent.trim()) {
        this.$message.warning('请先填写完整的标题和正文')
        return
      }

      const postData = {
        id: this.isEdit ? this.articleId : (this.form.abbrlink || undefined),
        title: this.form.title,
        tags: (this.parsedFrontMatter.tags && this.parsedFrontMatter.tags.length > 0)
          ? this.parsedFrontMatter.tags
          : (this.form.tags ? this.form.tags.split(',').map((tag) => tag.trim()).filter(Boolean) : []),
        category: (this.parsedFrontMatter.categories && this.parsedFrontMatter.categories.length > 0)
          ? this.parsedFrontMatter.categories
          : (this.form.category || null),
        description: this.form.description,
        cover: this.form.cover,
        date: this.form.date || null,
        updated: this.form.updated || null,
        content: this.markdownContent
      }

      try {
        if (this.isEdit) {
          await updatePost(postData)
          this.$message.success('文章更新成功')
        } else {
          await addPost(postData)
          this.$message.success('文章提交成功')
        }
        this.$router.push('/article')
      } catch (error) {
        console.error(this.isEdit ? '更新文章失败:' : '新增文章失败:', error)
        this.$message.error(this.isEdit ? '更新文章失败，请稍后重试' : '新增文章失败，请稍后重试')
      }
    },
    resetForm() {
      this.form = createEmptyForm()
      this.parsedFrontMatter = {
        tags: [],
        categories: []
      }
      this.markdownContent = ''
      this.renderedHtml = ''
    }
  }
}
</script>

<style scoped>
.add-article {
  padding: 16px;
  min-height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  width: 100%;
}

.form-section {
  margin-bottom: 12px;
}

.article-form {
  max-width: 760px;
}

.add-article h2 {
  margin: 0 0 8px;
  background-image: linear-gradient(110deg, #fff5e0 0%, #edd39d 42%, #d6ad5c 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.form-input-row {
  width: 100%;
  display: flex;
  gap: 10px;
}

.form-input-row :deep(.el-input) {
  flex: 1;
}

.editor-toolbar {
  margin-top: 8px;
}

.editor-container {
  display: flex;
  flex: 1;
  gap: 16px;
  margin-top: 10px;
  width: 100%;
  min-height: 360px;
}

.editor-left,
.editor-right {
  flex: 1;
  height: 100%;
  overflow: auto;
  border: 1px solid var(--admin-border);
  padding: 10px;
  border-radius: 14px;
  background: rgba(255, 251, 245, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.editor-left textarea {
  width: 100%;
  height: 100%;
  resize: none;
}

.editor-right {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(248, 240, 229, 0.96));
  color: var(--admin-text);
}

.editor-right img {
  max-width: 300px;
  max-height: 200px;
  object-fit: contain;
}

.submit-section {
  margin-top: 16px;
  text-align: center;
}

@media (max-width: 768px) {
  .article-form {
    max-width: 100%;
  }

  .form-input-row {
    flex-direction: column;
  }

  .form-input-row .el-button,
  .editor-toolbar .el-button {
    width: 100%;
  }

  .editor-container {
    flex-direction: column;
    min-height: 0;
  }

  .editor-left,
  .editor-right {
    min-height: 260px;
  }

  .submit-section .el-button {
    width: 100%;
  }

  .submit-section .el-button + .el-button {
    margin-top: 10px;
    margin-left: 0;
  }
}
</style>
