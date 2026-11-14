<template>
  <div class="main-layout" :class="{ 'sidebar-collapsed': isCollapsed }">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon :size="28"><Monitor /></el-icon>
          <span v-show="!isCollapsed" class="logo-text">GameStats</span>
        </div>
        <el-button
          class="collapse-btn"
          text
          @click="isCollapsed = !isCollapsed"
        >
          <el-icon :size="18">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </el-button>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span v-show="!isCollapsed" class="nav-text">{{ item.title }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <el-dropdown
          v-if="userStore.userInfo"
          trigger="click"
          placement="top-start"
          popper-class="account-menu"
          @command="handleMenuCommand"
        >
          <div class="user-info">
            <el-avatar :size="36" :src="userStore.userInfo.avatar">
              {{ userStore.userInfo.username?.charAt(0) }}
            </el-avatar>
            <div v-show="!isCollapsed" class="user-details">
              <span class="username">{{ userStore.userInfo.username }}</span>
              <span class="user-level">Lv.{{ userStore.userInfo.level || 1 }}</span>
            </div>
            <el-icon v-show="!isCollapsed" class="menu-arrow"><ArrowUp /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="account">
                <el-icon><Setting /></el-icon> 账号设置
              </el-dropdown-item>
              <el-dropdown-item command="preference">
                <el-icon><Brush /></el-icon> 偏好设置
              </el-dropdown-item>
              <el-dropdown-item command="password">
                <el-icon><Lock /></el-icon> 修改密码
              </el-dropdown-item>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon> 个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout" class="logout-item">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <el-input
            v-model="searchQuery"
            placeholder="搜索游戏..."
            prefix-icon="Search"
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          />
          <NotificationCenter />
        </div>
      </header>

      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- 账号菜单子弹窗 -->
    <AccountSettingsDialog v-model="accountDialogVisible" />
    <PreferenceDialog v-model="preferenceDialogVisible" />
    <ChangePasswordDialog v-model="passwordDialogVisible" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usePreferencesStore } from '@/store/preferences'
import { useNotificationStore } from '@/store/notification'
import AccountSettingsDialog from '@/components/AccountSettingsDialog.vue'
import PreferenceDialog from '@/components/PreferenceDialog.vue'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import NotificationCenter from '@/components/NotificationCenter.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const preferencesStore = usePreferencesStore()
const notificationStore = useNotificationStore()

// 侧边栏默认折叠状态来自偏好（服务端真源，缓存兜底首屏）
const isCollapsed = ref(!!preferencesStore.state.sidebarCollapsed)
const searchQuery = ref('')

// 用户手动折叠只改本地 isCollapsed，不回写偏好；
// 仅当偏好本身变化（偏好设置保存/服务端覆盖）时才同步默认值
watch(
  () => preferencesStore.state.sidebarCollapsed,
  (val) => {
    isCollapsed.value = !!val
  }
)

// 账号菜单子弹窗显隐
const accountDialogVisible = ref(false)
const preferenceDialogVisible = ref(false)
const passwordDialogVisible = ref(false)

const menuItems = [
  { path: '/dashboard', title: '数据中心', icon: 'DataAnalysis' },
  { path: '/library', title: '游戏库', icon: 'Grid' },
  { path: '/social', title: '社交', icon: 'UserFilled' },
  { path: '/year-report', title: '年度报告', icon: 'Calendar' },
  { path: '/profile', title: '个人中心', icon: 'Setting' }
]

const pageTitle = computed(() => {
  // 路由 meta.title 优先（子路由显式声明，父路由不设以免覆盖）
  if (route.meta?.title) return route.meta.title
  // 游戏详情为动态路由，不在菜单项中
  if (route.path.startsWith('/game/')) return '游戏详情'
  const item = menuItems.find(m => m.path === route.path)
  return item?.title || '首页'
})

function isActive(path) {
  return route.path === path
}

function handleSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/library', query: { search: searchQuery.value } })
  }
}

// 账号菜单命令分发：对应菜单项 → 弹窗 / 跳转 / 退出
function handleMenuCommand(command) {
  switch (command) {
    case 'account':
      accountDialogVisible.value = true
      break
    case 'preference':
      preferenceDialogVisible.value = true
      break
    case 'password':
      passwordDialogVisible.value = true
      break
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 退出登录二次确认，防误触
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    router.push('/login')
  } catch {
    // 用户取消，不做任何操作
  }
}

// 内页挂载：同步服务端偏好（服务端为真源）并启动通知轮询
onMounted(() => {
  preferencesStore.load().catch(() => {
    // 偏好加载失败保留本地缓存，不打断页面
  })
  notificationStore.startPolling()
})

// 离开内页清理轮询定时器与 visibilitychange 监听
onUnmounted(() => {
  notificationStore.stopPolling()
})
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  background: $bg-dark;
  padding-left: $sidebar-width;
  transition: padding-left $transition-normal;

  &.sidebar-collapsed {
    padding-left: 72px;
  }
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: $sidebar-width;
  height: 100vh;
  z-index: 100;
  background: $bg-darker;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  transition: width $transition-normal;

  &.collapsed {
    width: 72px;

    .logo-text,
    .nav-text,
    .user-details,
    .logout-btn {
      display: none;
    }
  }
}

.sidebar-header {
  height: $header-height;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid $border-color;
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  color: $primary-color;

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    background: linear-gradient(135deg, $primary-color, $secondary-color);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.collapse-btn {
  color: $text-secondary;

  &:hover {
    color: $text-primary;
  }
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: $radius-md;
  color: $text-secondary;
  transition: all $transition-fast;

  &:hover {
    background: $bg-card;
    color: $text-primary;
  }

  &.active {
    background: rgba($primary-color, 0.15);
    color: $primary-color;

    .el-icon {
      color: $primary-color;
    }
  }
}

.sidebar-footer {
  position: sticky;
  bottom: 0;
  padding: 16px;
  border-top: 1px solid $border-color;
  background: $bg-darker;

  // el-dropdown 默认 inline-block，占满 footer 宽度以扩大点击区域
  :deep(.el-dropdown) {
    display: block;
    width: 100%;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: $radius-md;
  background: $bg-card;
  cursor: pointer;
  transition: all $transition-normal;
  outline: none;

  &:hover {
    background: $bg-card-hover;
    transform: translateY(-2px);
    box-shadow: $shadow-md;
  }

  .menu-arrow {
    margin-left: auto;
    color: $text-muted;
    font-size: 12px;
    transition: color $transition-fast;
  }

  &:hover .menu-arrow {
    color: $primary-color;
  }
}

.user-details {
  display: flex;
  flex-direction: column;

  .username {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
  }

  .user-level {
    font-size: 12px;
    color: $primary-color;
  }
}

.logout-btn {
  width: 100%;
  margin-top: 12px;
  color: $text-muted;

  &:hover {
    color: $danger-color;
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: $header-height;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg-darker;
  border-bottom: 1px solid $border-color;
}

.header-left {
  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: $text-primary;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-input {
  width: 280px;
}

.icon-btn {
  color: $text-secondary;

  &:hover {
    color: $text-primary;
  }
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
