<template>
  <div class="sidebar" :class="{ 'is-mobile': isMobile, 'is-visible': visible }">
    <div class="brand">GodLeiBlog Admin</div>
    <el-menu
      :default-active="$route.path"
      class="el-menu-vertical"
      @select="handleSelect"
    >
      <el-menu-item index="/">首页</el-menu-item>
      <el-menu-item index="/article">文章管理</el-menu-item>
      <el-menu-item index="/category">分类管理</el-menu-item>
      <el-menu-item index="/tag">标签管理</el-menu-item>
      <el-menu-item index="/links">友链管理</el-menu-item>
      <el-menu-item index="/logs">日志管理</el-menu-item>
      <el-menu-item index="/site-settings">站点配置</el-menu-item>
    </el-menu>
  </div>
</template>

<script>
export default {
  props: {
    isMobile: {
      type: Boolean,
      default: false
    },
    visible: {
      type: Boolean,
      default: true
    }
  },
  methods: {
    handleSelect(index) {
      this.$router.push(index)
      if (this.isMobile) {
        this.$emit('close')
      }
    }
  }
}
</script>

<style>
.sidebar {
  width: 220px;
  height: 100vh;
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  border-bottom: 1px solid #eef2f7;
}

.el-menu-vertical {
  border-right: none;
  padding-top: 8px;
}

.el-menu-vertical .el-menu-item {
  color: #4b5563;
}

.el-menu-vertical .el-menu-item:hover {
  background: #f8fafc;
}

.el-menu-vertical .el-menu-item.is-active {
  color: #111827;
  font-weight: 600;
  background: #f5f7fb;
}

@media (max-width: 768px) {
  .sidebar.is-mobile {
    position: fixed;
    left: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: 0 10px 30px rgba(15, 23, 42, 0.22);
  }

  .sidebar.is-mobile.is-visible {
    transform: translateX(0);
  }
}
</style>
