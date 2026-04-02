import { createRouter, createWebHistory } from 'vue-router'

const Home = () => import('@/views/Home.vue')
const Article = () => import('@/views/Article.vue')
const Category = () => import('@/views/Category.vue')
const Tag = () => import('@/views/Tag.vue')
const AddArticle = () => import('@/views/AddArticle.vue')
const Contact = () => import('@/views/Contact.vue')
const Login = () => import('@/views/Login.vue')
const Links = () => import('@/views/Links.vue')
const Logs = () => import('@/views/Logs.vue')
const LogsView = () => import('@/views/LogsView.vue')
const SiteSettings = () => import('@/views/SiteSettings.vue')
const HomeSettings = () => import('@/views/HomeSettings.vue')
const AboutSettings = () => import('@/views/AboutSettings.vue')
const AssistantSettings = () => import('@/views/AssistantSettings.vue')
const Moments = () => import('@/views/Moments.vue')

const routes = [
  { path: '/login', name: 'Login', component: Login, meta: { requiresAuth: false } },
  { path: '/', name: 'Home', component: Home, meta: { requiresAuth: true, title: '控制台', description: '查看流量、最新评论和全站运行概况。', eyebrow: 'Dashboard' } },
  { path: '/article', name: 'Article', component: Article, meta: { requiresAuth: true, title: '文章管理', description: '集中管理文章列表、搜索与编辑入口。', eyebrow: 'Content' } },
  { path: '/category', name: 'Category', component: Category, meta: { requiresAuth: true, title: '分类管理', description: '维护文章分类树与分类下内容。', eyebrow: 'Taxonomy' } },
  { path: '/tag', name: 'Tag', component: Tag, meta: { requiresAuth: true, title: '标签管理', description: '维护文章标签和标签关联内容。', eyebrow: 'Taxonomy' } },
  { path: '/moments', name: 'Moments', component: Moments, meta: { requiresAuth: true, title: '朋友圈管理', description: '在左侧浏览动态列表，在右侧发布和编辑朋友圈内容。', eyebrow: 'Moments' } },
  { path: '/links', name: 'Links', component: Links, meta: { requiresAuth: true, title: '友链管理', description: '维护友链分组、友链资料与展示顺序。', eyebrow: 'Links' } },
  { path: '/logs', name: 'Logs', component: Logs, meta: { requiresAuth: true, title: '日志管理', description: '维护站点更新日志与当前生效版本。', eyebrow: 'Changelog' } },
  { path: '/site-settings', name: 'SiteSettings', component: SiteSettings, meta: { requiresAuth: true, title: '站点配置', description: '维护全站公共基础信息，包括站点名称和头像。', eyebrow: 'Settings' } },
  { path: '/home-settings', name: 'HomeSettings', component: HomeSettings, meta: { requiresAuth: true, title: '首页管理', description: '独立维护首页欢迎区、公告区和社交入口。', eyebrow: 'Settings' } },
  { path: '/about-settings', name: 'AboutSettings', component: AboutSettings, meta: { requiresAuth: true, title: '关于管理', description: '独立维护关于页展示所需的素材图列表。', eyebrow: 'Settings' } },
  { path: '/assistant-settings', name: 'AssistantSettings', component: AssistantSettings, meta: { requiresAuth: true, title: 'AI 助手', description: '维护馨宝的前台文案，并查看服务端运行状态。', eyebrow: 'Settings' } },
  { path: '/logs/content', name: 'LogsView', component: LogsView, meta: { requiresAuth: true, title: '日志预览', description: '查看整理后的日志内容输出。', eyebrow: 'Changelog' } },
  { path: '/add-article', name: 'AddArticle', component: AddArticle, meta: { requiresAuth: true, title: '发布文章', description: '编辑文章正文、封面、分类与标签。', eyebrow: 'Editor' } },
  { path: '/edit-article/:id', name: 'EditArticle', component: AddArticle, meta: { requiresAuth: true, title: '编辑文章', description: '继续完善文章内容并保存更新。', eyebrow: 'Editor' } },
  { path: '/contact', name: 'Contact', component: Contact, meta: { requiresAuth: true, title: '联系信息', description: '维护站点联系与外部入口。', eyebrow: 'Contact' } }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
