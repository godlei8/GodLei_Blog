<template>
  <div class="links-manage-page page-stack">
    <section class="page-card">
      <div class="page-stat-grid">
        <article class="page-stat-card">
          <span class="page-stat-card__label">分类数量</span>
          <strong class="page-stat-card__value">{{ classList.length }}</strong>
          <span class="page-stat-card__hint">友链分类用于组织前台展示结构。</span>
        </article>

        <article class="page-stat-card">
          <span class="page-stat-card__label">友链数量</span>
          <strong class="page-stat-card__value">{{ linkList.length }}</strong>
          <span class="page-stat-card__hint">当前列表会随分类与关键词过滤变化。</span>
        </article>

        <article class="page-stat-card">
          <span class="page-stat-card__label">当前视图</span>
          <strong class="page-stat-card__value links-stat-text">{{ activeTabLabel }}</strong>
          <span class="page-stat-card__hint">在同一页内切换分类管理和友链管理。</span>
        </article>
      </div>
    </section>

    <section class="page-card">
      <el-tabs v-model="activeTab" class="links-tabs">
        <el-tab-pane label="友链分类管理" name="class">
          <div class="page-stack">
            <div class="page-toolbar page-toolbar--split">
              <div class="page-toolbar__group">
                <el-input
                  v-model="classKeyword"
                  placeholder="输入分类名称"
                  clearable
                  class="toolbar-input"
                  @keyup.enter="fetchClassList"
                />
              </div>

              <div class="page-toolbar__group">
                <el-button type="primary" @click="fetchClassList">搜索</el-button>
                <el-button type="success" @click="openClassAddDialog">新增分类</el-button>
              </div>
            </div>

            <div class="section-head">
              <div class="section-head__copy">
                <h3 class="section-head__title">分类列表</h3>
                <p class="section-head__desc">管理友链分类名称、说明和排序，便于前台集中展示。</p>
              </div>
            </div>

            <div class="table-shell">
              <div class="table-wrap">
                <el-table :data="classList" stripe style="width: 100%" empty-text="暂无友链分类">
                  <el-table-column prop="id" label="ID" width="90" />
                  <el-table-column prop="className" label="分类名称" min-width="180" />
                  <el-table-column prop="classDesc" label="分类描述" min-width="240" show-overflow-tooltip />
                  <el-table-column prop="sort" label="排序" width="100" />
                  <el-table-column label="操作" width="180" fixed="right">
                    <template #default="scope">
                      <div class="row-actions">
                        <el-button size="small" type="primary" @click="openClassEditDialog(scope.row)">编辑</el-button>
                        <el-button size="small" type="danger" @click="removeClass(scope.row)">删除</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="友链管理" name="link">
          <div class="page-stack">
            <div class="page-toolbar page-toolbar--split">
              <div class="page-toolbar__group">
                <el-select
                  v-model="linkClassId"
                  clearable
                  placeholder="按分类筛选"
                  class="toolbar-select"
                >
                  <el-option
                    v-for="item in classList"
                    :key="item.id"
                    :label="item.className"
                    :value="item.id"
                  />
                </el-select>
                <el-input
                  v-model="linkKeyword"
                  placeholder="输入站点名称或链接"
                  clearable
                  class="toolbar-input"
                  @keyup.enter="fetchLinkList"
                />
              </div>

              <div class="page-toolbar__group">
                <el-button type="primary" @click="fetchLinkList">搜索</el-button>
                <el-button type="success" @click="openLinkAddDialog">新增友链</el-button>
              </div>
            </div>

            <div class="section-head">
              <div class="section-head__copy">
                <h3 class="section-head__title">友链列表</h3>
                <p class="section-head__desc">维护友链的分类、站点信息、头像、描述和展示顺序。</p>
              </div>
            </div>

            <div class="table-shell">
              <div class="table-wrap">
                <el-table :data="linkList" stripe style="width: 100%" empty-text="暂无友链数据">
                  <el-table-column prop="id" label="ID" width="90" />
                  <el-table-column prop="className" label="分类" width="140" />
                  <el-table-column prop="name" label="站点名称" min-width="160" />
                  <el-table-column label="头像" width="110">
                    <template #default="scope">
                      <el-image
                        v-if="scope.row.avatar"
                        :src="scope.row.avatar"
                        fit="cover"
                        class="link-avatar"
                        :preview-src-list="[scope.row.avatar]"
                        preview-teleported
                      />
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="link" label="链接" min-width="220" show-overflow-tooltip>
                    <template #default="scope">
                      <a :href="scope.row.link" target="_blank" rel="noopener noreferrer">{{ scope.row.link }}</a>
                    </template>
                  </el-table-column>
                  <el-table-column prop="sort" label="排序" width="90" />
                  <el-table-column label="操作" width="180" fixed="right">
                    <template #default="scope">
                      <div class="row-actions">
                        <el-button size="small" type="primary" @click="openLinkEditDialog(scope.row)">编辑</el-button>
                        <el-button size="small" type="danger" @click="removeLink(scope.row)">删除</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog :title="isClassEdit ? '编辑分类' : '新增分类'" v-model="classDialogVisible" width="520px">
      <el-form :model="classForm" label-width="90px">
        <el-form-item label="分类名称">
          <el-input v-model="classForm.className" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类描述">
          <el-input v-model="classForm.classDesc" placeholder="请输入分类描述" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="classForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitClassForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog :title="isLinkEdit ? '编辑友链' : '新增友链'" v-model="linkDialogVisible" width="640px">
      <el-form :model="linkForm" label-width="90px">
        <el-form-item label="友链分类">
          <el-select v-model="linkForm.classId" placeholder="请选择分类" style="width: 100%;">
            <el-option
              v-for="item in classList"
              :key="item.id"
              :label="item.className"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="站点名称">
          <el-input v-model="linkForm.name" placeholder="请输入站点名称" />
        </el-form-item>
        <el-form-item label="站点链接">
          <el-input v-model="linkForm.link" placeholder="请输入站点链接" />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="linkForm.avatar" placeholder="请输入头像 URL（可选）" />
        </el-form-item>
        <el-form-item label="站点描述">
          <el-input v-model="linkForm.descr" placeholder="请输入描述（可选）" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="RSS 地址">
          <el-input v-model="linkForm.rss" placeholder="请输入 RSS 链接（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="linkForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLinkForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  addLink,
  addLinkClass,
  deleteLink,
  deleteLinkClass,
  getLinkClassList,
  getLinkList,
  updateLink,
  updateLinkClass
} from '@/api'

export default {
  data() {
    return {
      activeTab: 'class',
      classKeyword: '',
      linkKeyword: '',
      linkClassId: null,
      classList: [],
      linkList: [],
      classDialogVisible: false,
      linkDialogVisible: false,
      isClassEdit: false,
      isLinkEdit: false,
      classForm: {
        id: null,
        className: '',
        classDesc: '',
        sort: 0
      },
      linkForm: {
        id: null,
        classId: null,
        name: '',
        link: '',
        avatar: '',
        descr: '',
        rss: '',
        sort: 0
      }
    }
  },
  computed: {
    activeTabLabel() {
      return this.activeTab === 'class' ? '分类管理' : '友链管理'
    }
  },
  mounted() {
    this.fetchClassList()
    this.fetchLinkList()
  },
  methods: {
    async fetchClassList() {
      try {
        const data = await getLinkClassList({ keyword: this.classKeyword || undefined })
        this.classList = data || []
      } catch (error) {
        console.error('获取友链分类失败:', error)
        this.$message.error('获取友链分类失败')
      }
    },
    async fetchLinkList() {
      try {
        const data = await getLinkList({
          classId: this.linkClassId || undefined,
          keyword: this.linkKeyword || undefined
        })
        this.linkList = data || []
      } catch (error) {
        console.error('获取友链列表失败:', error)
        this.$message.error('获取友链列表失败')
      }
    },
    openClassAddDialog() {
      this.isClassEdit = false
      this.classForm = { id: null, className: '', classDesc: '', sort: 0 }
      this.classDialogVisible = true
    },
    openClassEditDialog(row) {
      this.isClassEdit = true
      this.classForm = {
        id: row.id,
        className: row.className || '',
        classDesc: row.classDesc || '',
        sort: row.sort ?? 0
      }
      this.classDialogVisible = true
    },
    async submitClassForm() {
      if (!this.classForm.className || !this.classForm.className.trim()) {
        this.$message.warning('请输入分类名称')
        return
      }
      const payload = {
        id: this.classForm.id,
        className: this.classForm.className.trim(),
        classDesc: this.classForm.classDesc,
        sort: this.classForm.sort
      }
      try {
        if (this.isClassEdit) {
          await updateLinkClass(payload)
          this.$message.success('分类更新成功')
        } else {
          await addLinkClass(payload)
          this.$message.success('分类新增成功')
        }
        this.classDialogVisible = false
        this.fetchClassList()
      } catch (error) {
        console.error('保存友链分类失败:', error)
        this.$message.error('保存友链分类失败')
      }
    },
    removeClass(row) {
      this.$confirm(`确定删除分类 "${row.className}" 吗？删除前请先删除其下所有友链。`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          await deleteLinkClass({ id: row.id })
          this.$message.success('删除成功')
          this.fetchClassList()
          this.fetchLinkList()
        })
        .catch(() => {})
    },
    openLinkAddDialog() {
      this.isLinkEdit = false
      this.linkForm = {
        id: null,
        classId: this.linkClassId || null,
        name: '',
        link: '',
        avatar: '',
        descr: '',
        rss: '',
        sort: 0
      }
      this.linkDialogVisible = true
    },
    openLinkEditDialog(row) {
      this.isLinkEdit = true
      this.linkForm = {
        id: row.id,
        classId: row.classId,
        name: row.name || '',
        link: row.link || '',
        avatar: row.avatar || '',
        descr: row.descr || '',
        rss: row.rss || '',
        sort: row.sort ?? 0
      }
      this.linkDialogVisible = true
    },
    async submitLinkForm() {
      if (!this.linkForm.classId) {
        this.$message.warning('请选择友链分类')
        return
      }
      if (!this.linkForm.name || !this.linkForm.name.trim()) {
        this.$message.warning('请输入站点名称')
        return
      }
      if (!this.linkForm.link || !this.linkForm.link.trim()) {
        this.$message.warning('请输入站点链接')
        return
      }
      const payload = {
        id: this.linkForm.id,
        classId: this.linkForm.classId,
        name: this.linkForm.name.trim(),
        link: this.linkForm.link.trim(),
        avatar: this.linkForm.avatar,
        descr: this.linkForm.descr,
        rss: this.linkForm.rss,
        sort: this.linkForm.sort
      }
      try {
        if (this.isLinkEdit) {
          await updateLink(payload)
          this.$message.success('友链更新成功')
        } else {
          await addLink(payload)
          this.$message.success('友链新增成功')
        }
        this.linkDialogVisible = false
        this.fetchLinkList()
      } catch (error) {
        console.error('保存友链失败:', error)
        this.$message.error('保存友链失败')
      }
    },
    removeLink(row) {
      this.$confirm(`确定删除友链 "${row.name}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          await deleteLink({ id: row.id })
          this.$message.success('删除成功')
          this.fetchLinkList()
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.links-stat-text {
  font-size: 20px;
}

.link-avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(214, 173, 92, 0.12);
}
</style>
