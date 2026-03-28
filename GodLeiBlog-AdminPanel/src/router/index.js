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

const routers = [
  { path: '/login', name: 'Login', component: Login, meta: { requiresAuth: false } },
  { path: '/', name: 'Home', component: Home, meta: { requiresAuth: true } },
  { path: '/article', name: 'Article', component: Article, meta: { requiresAuth: true } },
  { path: '/category', name: 'Category', component: Category, meta: { requiresAuth: true } },
  { path: '/tag', name: 'Tag', component: Tag, meta: { requiresAuth: true } },
  { path: '/links', name: 'Links', component: Links, meta: { requiresAuth: true } },
  { path: '/logs', name: 'Logs', component: Logs, meta: { requiresAuth: true } },
  { path: '/site-settings', name: 'SiteSettings', component: SiteSettings, meta: { requiresAuth: true } },
  { path: '/logs/content', name: 'LogsView', component: LogsView, meta: { requiresAuth: true } },
  { path: '/add-article', name: 'AddArticle', component: AddArticle, meta: { requiresAuth: true } },
  { path: '/edit-article/:id', name: 'EditArticle', component: AddArticle, meta: { requiresAuth: true } },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routers
})

// 路由守卫 - 未登录拦截
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录，跳转登录页
    next('/login')
  } else {
    next()
  }
})

export default router
