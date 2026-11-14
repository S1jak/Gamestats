import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

// 未读数轮询间隔：60s，仅页面可见时轮询
export const NOTIFICATION_POLL_INTERVAL = 60000
export const NOTIFICATION_PAGE_SIZE = 20

function normalizeItem(raw) {
  const item = raw || {}
  return {
    id: item.id,
    type: item.type || 'system',
    title: item.title || '',
    content: item.content || '',
    linkUrl: item.linkUrl || '',
    isRead: item.isRead === true || item.isRead === 1 || item.isRead === '1',
    createdAt: item.createdAt || ''
  }
}

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const items = ref([])
  const loading = ref(false)
  const hasMore = ref(false)

  let timer = null
  let visibilityHandler = null

  function clearTimer() {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  // 未读数：失败保留旧值，不弹错误（避免每 60s 打扰用户）
  async function fetchUnread() {
    try {
      const data = await api.get('/notifications/unread-count')
      const count = Number(data?.count)
      unreadCount.value = Number.isFinite(count) && count > 0 ? count : 0
    } catch (e) {
      // 401 由 api.js 统一处理；其他失败保留上次未读数
    }
  }

  // 列表：reset=true 取首页，false 以最后一条 id 为游标追加
  async function fetchList(reset = true) {
    if (loading.value) return
    loading.value = true
    try {
      const params = { limit: NOTIFICATION_PAGE_SIZE }
      if (!reset && items.value.length) {
        params.before = items.value[items.value.length - 1].id
      }
      const list = await api.get('/notifications', { params })
      const normalized = (Array.isArray(list) ? list : []).map(normalizeItem)
      items.value = reset ? normalized : items.value.concat(normalized)
      hasMore.value = normalized.length >= NOTIFICATION_PAGE_SIZE
    } catch (e) {
      // 错误提示由 api.js 统一处理，列表保持原值
    } finally {
      loading.value = false
    }
  }

  // 单条已读：本地同步 isRead 与计数，失败时抛出由调用方决定是否跳转
  async function markRead(id) {
    await api.put(`/notifications/${id}/read`)
    const target = items.value.find((item) => item.id === id)
    if (target && !target.isRead) {
      target.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  // 全部已读：成功后本地全部置读、未读数归零
  async function markAllRead() {
    await api.put('/notifications/read-all')
    items.value.forEach((item) => {
      item.isRead = true
    })
    unreadCount.value = 0
  }

  function startTimer() {
    clearTimer()
    timer = setInterval(() => {
      fetchUnread()
    }, NOTIFICATION_POLL_INTERVAL)
  }

  // 页面隐藏暂停、恢复后立即拉一次并重启定时器
  function startPolling() {
    stopPolling()
    fetchUnread()
    if (typeof document === 'undefined') return
    visibilityHandler = () => {
      if (document.visibilityState === 'hidden') {
        clearTimer()
      } else {
        fetchUnread()
        startTimer()
      }
    }
    document.addEventListener('visibilitychange', visibilityHandler)
    if (document.visibilityState !== 'hidden') {
      startTimer()
    }
  }

  function stopPolling() {
    clearTimer()
    if (visibilityHandler && typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', visibilityHandler)
    }
    visibilityHandler = null
  }

  return {
    unreadCount,
    items,
    loading,
    hasMore,
    fetchUnread,
    fetchList,
    markRead,
    markAllRead,
    startPolling,
    stopPolling
  }
})