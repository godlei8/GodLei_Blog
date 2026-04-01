import { createRouter, createWebHistory } from 'vue-router';

// 引入页面组件
const Home = () => import('@/views/Home.vue');
const About = () => import('@/views/About.vue');
const Archive = () => import('@/views/Archive.vue');
const Links = () => import('@/views/Links.vue');
const Comments = () => import('@/views/Comments.vue');
const Logs = () => import('@/views/Logs.vue');
const PostDetail = () => import('@/views/PostDetail.vue');


const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/about', name: 'About', component: About },
  { path: '/archive', name: 'Archive', component: Archive },
  { path: '/links', name: 'Links', component: Links },
  { path: '/comments', name: 'Comments', component: Comments },
  { path: '/logs', name: 'Logs', component: Logs },
  {
    path: '/posts/:id',
    name: 'PostDetail',
    component: PostDetail,
    props: true // 允许通过 props 接收路由参数
  }

];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }

    // 点击首页文章卡片进入详情页时，始终从顶部开始
    if (to.name === 'PostDetail') {
      return { top: 0, left: 0 };
    }

    return { top: 0, left: 0 };
  }
});

export default router;
