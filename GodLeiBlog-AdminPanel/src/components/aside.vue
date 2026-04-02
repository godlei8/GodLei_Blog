<template>
  <aside class="sidebar" :class="{ 'is-mobile': isMobile, 'is-visible': visible }">
    <div class="brand">
      <span class="brand__eyebrow">Admin Panel</span>
      <strong>GodLei Blog</strong>
    </div>

    <el-menu
      :default-active="$route.path"
      class="el-menu-vertical"
      @select="handleSelect"
    >
      <el-menu-item index="/">控制台</el-menu-item>
      <el-menu-item index="/article">文章管理</el-menu-item>
      <el-menu-item index="/category">分类管理</el-menu-item>
      <el-menu-item index="/tag">标签管理</el-menu-item>
      <el-menu-item index="/moments">朋友圈管理</el-menu-item>
      <el-menu-item index="/links">友链管理</el-menu-item>
      <el-menu-item index="/logs">日志管理</el-menu-item>
      <el-menu-item index="/site-settings">站点配置</el-menu-item>
      <el-menu-item index="/home-settings">首页管理</el-menu-item>
      <el-menu-item index="/about-settings">关于管理</el-menu-item>
      <el-menu-item index="/assistant-settings">AI 助手</el-menu-item>
    </el-menu>
  </aside>
</template>

<script>
export default {
  props: {
    isMobile: {
      type: Boolean,
      default: false,
    },
    visible: {
      type: Boolean,
      default: true,
    },
  },
  methods: {
    handleSelect(index) {
      this.$router.push(index)
      if (this.isMobile) {
        this.$emit('close')
      }
    },
  },
}
</script>

<style>
.sidebar {
  width: 228px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.92);
  border-right: 1px solid rgba(214, 173, 92, 0.14);
  position: sticky;
  top: 0;
  backdrop-filter: blur(16px);
  box-shadow: 10px 0 24px rgba(112, 84, 34, 0.04);
}

.brand {
  height: 74px;
  display: grid;
  align-content: center;
  gap: 4px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(214, 173, 92, 0.14);
}

.brand__eyebrow {
  color: var(--admin-text-soft);
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.brand strong {
  color: var(--admin-text);
  font-size: 18px;
}

.el-menu-vertical {
  border-right: none;
  padding: 10px 10px 18px;
  background: transparent;
}

.el-menu-vertical .el-menu-item {
  min-height: 44px;
  margin-bottom: 4px;
  border-radius: 12px;
  color: var(--admin-text-muted);
}

.el-menu-vertical .el-menu-item:hover {
  background: rgba(214, 173, 92, 0.1);
}

.el-menu-vertical .el-menu-item.is-active {
  color: #6b4a18;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(214, 173, 92, 0.18), rgba(250, 241, 221, 0.9));
}

@media (max-width: 768px) {
  .sidebar.is-mobile {
    position: fixed;
    left: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
  }

  .sidebar.is-mobile.is-visible {
    transform: translateX(0);
  }
}
</style>
