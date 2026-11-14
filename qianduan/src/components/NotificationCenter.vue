<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom-end"
    :width="384"
    trigger="click"
    popper-class="notification-popper"
  >
    <template #reference>
      <el-badge
        :value="unreadCount"
        :max="99"
        :hidden="!unreadCount"
        class="notification-badge"
      >
        <el-button text class="notification-bell">
          <el-icon :size="20"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="notification-panel">
      <div class="notification-header">
        <span class="notification-title">通知</span>
        <el-button
          text
          size="small"
          :disabled="!unreadCount"
          @click="handleMarkAllRead"
        >
          全部已读
        </el-button>
      </div>

      <div v-loading="loading" class="notification-list">
        <div v-if="!items.length && !loading" class="notification-empty">暂无通知</div>
        <div
          v-for="item in items"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.isRead }"
          @click="handleItemClick(item)"
        >
          <div class="item-icon">
            <el-icon :size="16"><component :is="iconOf(item.type)" /></el-icon>
          </div>
          <div class="item-body">
            <div class="item-title-row">
              <span class="item-title">{{ item.title }}</span>
              <span class="item-time">{{ relativeTime(item.createdAt) }}</span>
            </div>
            <p class="item-content">{{ item.content }}</p>
          </div>
          <span v-if="!item.isRead" class="unread-dot"></span>
        </div>
      </div>

      <div v-if="hasMore && items.length" class="notification-more">
        <el-button text size="small" :loading="loading" @click="handleLoadMore">
          查看更多
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useNotificationStore } from '@/store/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const visible = ref(false)

// storeToRefs 保留 ref 响应性（直接解构 store 会丢失响应式）
const { unreadCount, items, loading, hasMore } = storeToRefs(notificationStore)

watch(visible, (val) => {
  if (val) {
    notificationStore.fetchList(true)
  }
})

function iconOf(type) {
  switch (type) {
    case 'friend_request':
      return 'UserFilled'
    case 'friend_accepted':
      return 'User'
    case 'report_generated':
      return 'Calendar'
    default:
      return 'Bell'
  }
}

function relativeTime(value) {
  if (!value) return ''
  const target = dayjs(value)
  if (!target.isValid()) return value
  const diffMinutes = dayjs().diff(target, 'minute')
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  if (diffMinutes < 60 * 24) return `${Math.floor(diffMinutes / 60)}小时前`
  if (diffMinutes < 60 * 24 * 2) return '昨天'
  return target.format('MM-DD')
}

async function handleItemClick(item) {
  try {
    if (!item.isRead) {
      await notificationStore.markRead(item.id)
    }
  } catch (e) {
    // 404/401 已由 api.js 统一提示，点击后仍允许跳转
  }
  if (item.linkUrl) {
    visible.value = false
    router.push(item.linkUrl)
  }
}

async function handleMarkAllRead() {
  try {
    await notificationStore.markAllRead()
  } catch (e) {
    // 错误提示由 api.js 统一处理
  }
}

function handleLoadMore() {
  notificationStore.fetchList(false)
}
</script>

<style lang="scss" scoped>
.notification-bell {
  color: $text-secondary;

  &:hover {
    color: $text-primary;
  }
}
</style>

<!-- el-popover 内容 teleport 到 body，scoped 样式无法穿透，使用统一前缀的全局样式 -->
<style lang="scss">
.notification-popper {
  padding: 0 !important;
  background: #1a1a2e !important;
  border: 1px solid rgba(99, 102, 241, 0.3) !important;
  border-radius: 12px !important;
  box-shadow: 0 12px 40px rgba(91, 60, 196, 0.3), 0 4px 16px rgba(0, 0, 0, 0.5) !important;
}

.notification-panel {
  display: flex;
  flex-direction: column;
  max-height: 420px;

  .notification-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid rgba(99, 102, 241, 0.2);

    .notification-title {
      font-size: 14px;
      font-weight: 600;
      color: #ffffff;
    }
  }

  .notification-list {
    flex: 1;
    overflow-y: auto;
    min-height: 80px;
  }

  .notification-empty {
    padding: 40px 0;
    text-align: center;
    font-size: 13px;
    color: #71717a;
  }

  .notification-item {
    position: relative;
    display: flex;
    gap: 10px;
    padding: 12px 16px;
    cursor: pointer;
    transition: background 0.2s ease;
    border-bottom: 1px solid rgba(99, 102, 241, 0.08);

    &:hover {
      background: rgba(99, 102, 241, 0.1);
    }

    &.unread {
      background: rgba(99, 102, 241, 0.06);

      .item-title {
        color: #ffffff;
        font-weight: 600;
      }
    }

    .item-icon {
      flex-shrink: 0;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      background: rgba(99, 102, 241, 0.15);
      color: #818cf8;
    }

    .item-body {
      flex: 1;
      min-width: 0;
    }

    .item-title-row {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 8px;
    }

    .item-title {
      font-size: 13px;
      color: #d4d4e8;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-time {
      flex-shrink: 0;
      font-size: 11px;
      color: #71717a;
    }

    .item-content {
      margin-top: 4px;
      font-size: 12px;
      color: #a1a1aa;
      line-height: 1.5;
      overflow: hidden;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .unread-dot {
      position: absolute;
      left: 6px;
      top: 50%;
      width: 6px;
      height: 6px;
      margin-top: -3px;
      border-radius: 50%;
      background: #6366f1;
    }
  }

  .notification-more {
    padding: 8px;
    text-align: center;
    border-top: 1px solid rgba(99, 102, 241, 0.15);
  }
}
</style>