import { createRouter, createWebHistory } from 'vue-router'

const Home = () => import('@/views/Home.vue')
const About = () => import('@/views/About.vue')
const Archive = () => import('@/views/Archive.vue')
const Moments = () => import('@/views/Moments.vue')
const Links = () => import('@/views/Links.vue')
const Comments = () => import('@/views/Comments.vue')
const Logs = () => import('@/views/Logs.vue')
const PostDetail = () => import('@/views/PostDetail.vue')

const routes = [
  { path: '/', name: 'Home', component: Home, meta: { pageType: 'home', title: '首页', summary: '博客首页，包含站点欢迎区与最新文章流。' } },
  { path: '/about', name: 'About', component: About, meta: { pageType: 'about', title: '关于', summary: '站点作者介绍、角色设定与个人偏好信息。' } },
  { path: '/archive', name: 'Archive', component: Archive, meta: { pageType: 'archive', title: '归档', summary: '按时间与条件检索博客文章归档。' } },
  { path: '/moments', name: 'Moments', component: Moments, meta: { pageType: 'moments', title: '朋友圈', summary: '站点动态流页面，展示最近发布的生活与项目片段。' } },
  { path: '/links', name: 'Links', component: Links, meta: { pageType: 'links', title: '友链', summary: '站点友链与友链留言页。' } },
  { path: '/comments', name: 'Comments', component: Comments, meta: { pageType: 'comments', title: '留言', summary: '全站留言汇总与互动页面。' } },
  { path: '/logs', name: 'Logs', component: Logs, meta: { pageType: 'logs', title: '日志', summary: '站点更新日志与版本变更记录。' } },
  {
    path: '/posts/:id',
    name: 'PostDetail',
    component: PostDetail,
    meta: { pageType: 'article', title: '文章', summary: '当前文章的 Markdown 正文、目录和评论区。' },
    props: true,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return {
        el: to.hash,
        top: 24,
        behavior: 'smooth',
      }
    }
    return { top: 0, left: 0 }
  },
})

export default router
