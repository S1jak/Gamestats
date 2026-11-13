import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据中心' }
      },
      {
        path: 'library',
        name: 'Library',
        component: () => import('@/views/Library.vue'),
        meta: { title: '游戏库' }
      },
      {
        path: 'game/:id',
        name: 'GameDetail',
        component: () => import('@/views/GameDetail.vue'),
        meta: { title: '游戏详情' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'social',
        name: 'Social',
        component: () => import('@/views/Social.vue'),
        meta: { title: '社交' }
      },
      {
        path: 'year-report',
        name: 'YearReport',
        component: () => import('@/views/YearReport.vue'),
        meta: { title: '年度报告' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 需要登录但未登录 → 强制跳转登录页
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录用户访问登录页 → 跳转到 dashboard
  if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
    return
  }

  next()
})

export default router
