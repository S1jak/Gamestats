import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
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
  // 可解析、未过期、且有 30s 余量才算登录
  const loggedIn = userStore.isTokenValid()

  // 需要登录但未登录/已过期 → 清态后回登录页并保留 redirect
  if (to.meta.requiresAuth && !loggedIn) {
    const hadToken = !!userStore.token
    // token 能解析出 exp 且已过期才提示"登录已过期"；
    // 无 token 或 token 无法解析（如手写 'abc'）静默跳转，避免误导提示
    const expired = userStore.tokenExpiresAt !== null && userStore.tokenExpiresAt <= Date.now()
    if (hadToken) {
      userStore.logout()
    }
    if (expired) {
      ElMessage.warning('登录已过期，请重新登录')
    }
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录（token 有效）访问登录页 → 跳转到 dashboard
  if (to.path === '/login' && loggedIn) {
    next('/dashboard')
    return
  }

  next()
})

export default router