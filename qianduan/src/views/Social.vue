<template>
  <div class="social-page">
    <div class="page-header">
      <div class="header-title">
        <h2>社交中心</h2>
        <div class="invite-code-card">
          <span class="invite-label">我的邀请码</span>
          <strong class="invite-code">GS-{{ inviteCode || '------' }}</strong>
          <el-button text size="small" :disabled="!inviteCode" @click="copyInviteCode">复制</el-button>
        </div>
      </div>
      <el-button type="primary" @click="openInviteDialog">
        <el-icon><Plus /></el-icon>
        添加好友
      </el-button>
    </div>

    <el-tabs v-model="activeTab">
      <!-- Tab1 好友 -->
      <el-tab-pane label="好友" name="friends">
        <div v-loading="friendsLoading" class="tab-panel">
          <div v-if="!friendsLoading && !friends.length" class="tab-empty">
            <el-empty description="还没有好友，用邀请码添加第一位好友" :image-size="80" />
          </div>
          <div v-else class="friends-grid">
            <div v-for="friend in friends" :key="friend.id" class="friend-card">
              <div class="friend-header">
                <el-avatar :size="48" :src="friend.avatar">
                  {{ (friend.username || '?').charAt(0) }}
                </el-avatar>
                <div class="friend-info">
                  <span class="friend-name">{{ friend.username }}</span>
                  <span class="friend-level">Lv.{{ friend.level || 1 }}</span>
                </div>
                <el-dropdown trigger="click" @command="(cmd) => handleFriendCommand(cmd, friend)">
                  <el-button text class="menu-btn">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="compare">数据对比</el-dropdown-item>
                      <el-dropdown-item divided command="remove">删除好友</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <div class="friend-meta">
                <span class="meta-label">最近活跃</span>
                <span class="meta-value">{{ formatDate(friend.lastActive) || '暂无记录' }}</span>
              </div>

              <div class="friend-stats">
                <div class="stat">
                  <span class="value">{{ friend.totalGames ?? 0 }}</span>
                  <span class="label">游戏</span>
                </div>
                <div class="stat">
                  <span class="value">{{ friend.totalHours ?? 0 }}h</span>
                  <span class="label">时长</span>
                </div>
                <div class="stat">
                  <span class="value">{{ friend.achievements ?? 0 }}</span>
                  <span class="label">成就</span>
                </div>
              </div>

              <div class="recent-games">
                <span class="label">最近游玩</span>
                <div class="game-icons">
                  <template v-for="game in friend.recentGames || []" :key="game.id">
                    <img
                      v-if="game.cover && !isCoverFailed(friend.id, game.id)"
                      :src="game.cover"
                      :alt="game.name"
                      :title="game.name"
                      @error="markCoverFailed(friend.id, game.id)"
                    />
                    <div v-else class="game-cover-placeholder" :title="game.name">
                      {{ (game.name || '?').charAt(0) }}
                    </div>
                  </template>
                  <span v-if="!(friend.recentGames || []).length" class="recent-empty">暂无记录</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab2 好友请求 -->
      <el-tab-pane label="好友请求" name="requests">
        <div class="tab-panel">
          <div class="tab-toolbar">
            <el-radio-group v-model="requestDirection" @change="loadRequests">
              <el-radio-button label="incoming">待处理</el-radio-button>
              <el-radio-button label="outgoing">已发出</el-radio-button>
            </el-radio-group>
            <el-button text size="small" :loading="requestsLoading" @click="loadRequests">刷新</el-button>
          </div>

          <div v-loading="requestsLoading">
            <div v-if="!requestsLoading && !requests.length" class="tab-empty">
              <el-empty
                :description="requestDirection === 'incoming' ? '暂无待处理请求' : '暂无已发出请求'"
                :image-size="80"
              />
            </div>
            <div v-else class="request-list">
              <div v-for="req in requests" :key="req.id" class="request-card">
                <el-avatar :size="40" :src="req.user?.avatar">
                  {{ (req.user?.username || '?').charAt(0) }}
                </el-avatar>
                <div class="request-body">
                  <div class="request-title">
                    <span class="request-name">{{ req.user?.username || '未知用户' }}</span>
                    <el-tag size="small" :type="statusTagType(req.status)">{{ requestStatusText(req) }}</el-tag>
                  </div>
                  <p v-if="req.message" class="request-message">{{ req.message }}</p>
                  <span class="request-time">{{ formatDate(req.createdAt) }}</span>
                </div>
                <div
                  v-if="requestDirection === 'incoming' && req.status === 'pending'"
                  class="request-actions"
                >
                  <el-button
                    type="primary"
                    size="small"
                    :loading="handlingId === req.id"
                    @click="handleRequest(req, 'accept')"
                  >
                    接受
                  </el-button>
                  <el-button
                    size="small"
                    :disabled="handlingId === req.id"
                    @click="handleRequest(req, 'reject')"
                  >
                    拒绝
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab3 分享记录 -->
      <el-tab-pane label="分享记录" name="shares">
        <div class="tab-panel">
          <div class="tab-toolbar">
            <el-radio-group v-model="shareScope" @change="loadShares">
              <el-radio-button label="mine">我的</el-radio-button>
              <el-radio-button label="friends">好友圈</el-radio-button>
            </el-radio-group>
            <el-button text size="small" :loading="sharesLoading" @click="loadShares">刷新</el-button>
          </div>

          <div v-loading="sharesLoading">
            <div v-if="!sharesLoading && !shares.length" class="tab-empty">
              <el-empty description="还没有分享记录，去年度报告生成一条吧" :image-size="80" />
            </div>
            <div v-else class="shares-grid">
              <div v-for="share in shares" :key="share.id" class="share-card">
                <div class="share-cover">
                  <img
                    v-if="share.imageUrl && !failedShareCovers[share.id]"
                    :src="share.imageUrl"
                    :alt="share.title"
                    @error="markShareCoverFailed(share.id)"
                  />
                  <div v-else class="share-cover-placeholder">{{ typeLabel(share.type).charAt(0) }}</div>
                </div>
                <div class="share-content">
                  <div class="share-badges">
                    <el-tag size="small" type="info">{{ typeLabel(share.type) }}</el-tag>
                    <el-tag size="small" effect="plain">{{ visibilityLabel(share.visibility) }}</el-tag>
                  </div>
                  <h4>{{ share.title }}</h4>
                  <p>{{ share.summary || '暂无摘要' }}</p>
                  <div class="share-meta">
                    <div class="share-author">
                      <el-avatar :size="20" :src="share.author?.avatar">
                        {{ (share.author?.username || '?').charAt(0) }}
                      </el-avatar>
                      <span>{{ share.author?.username || '未知用户' }}</span>
                    </div>
                    <span class="share-date">{{ formatDate(share.createdAt) }}</span>
                    <el-button
                      v-if="canDeleteShare(share)"
                      text
                      size="small"
                      class="delete-btn"
                      @click="deleteShare(share)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加好友（输入邀请码 + 附言） -->
    <el-dialog v-model="showInviteDialog" title="添加好友" width="420px" class="gs-dialog" align-center>
      <el-form label-position="top">
        <el-form-item label="好友邀请码">
          <el-input
            v-model="inviteCodeInput"
            placeholder="输入邀请码，如 GS-XXXXXX 或 XXXXXX"
            maxlength="20"
            size="large"
          />
        </el-form-item>
        <el-form-item label="附言（可选，最多 120 字）">
          <el-input
            v-model="inviteMessage"
            type="textarea"
            :rows="3"
            maxlength="120"
            show-word-limit
            placeholder="介绍一下自己，让对方更容易接受"
          />
        </el-form-item>
      </el-form>
      <p class="invite-hint">
        我的邀请码：<strong>GS-{{ inviteCode || '------' }}</strong>
      </p>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" :loading="sendingInvite" @click="submitInvite">发送请求</el-button>
      </template>
    </el-dialog>

    <!-- 数据对比弹窗 -->
    <el-dialog
      v-model="compareVisible"
      :title="compareFriend ? `数据对比 · ${compareFriend.username}` : '数据对比'"
      width="720px"
      class="gs-dialog"
      align-center
    >
      <div v-if="compareLoading" class="compare-loading">加载中…</div>
      <div v-else-if="compareError" class="compare-error">
        <el-icon :size="28"><Lock /></el-icon>
        <p>{{ compareError }}</p>
      </div>
      <div v-else-if="compareData" class="compare-body">
        <div class="compare-name-row">
          <div class="side-name">{{ compareData.me.username }}</div>
          <span class="vs">VS</span>
          <div class="side-name">{{ compareData.friend.username }}</div>
        </div>

        <div class="compare-metrics">
          <div v-for="metric in compareMetrics" :key="metric.key" class="compare-metric">
            <span class="metric-me">{{ metric.format(compareData.me[metric.key]) }}</span>
            <span class="metric-label">{{ metric.label }}</span>
            <span class="metric-friend">{{ metric.format(compareData.friend[metric.key]) }}</span>
          </div>
        </div>

        <div class="compare-genres">
          <div class="genre-column">
            <h4>我的类型分布</h4>
            <div
              v-for="genre in compareData.me.genres"
              :key="`me-${genre.name}`"
              class="genre-line"
            >
              <span class="genre-name">{{ genre.name }}</span>
              <div class="genre-track">
                <div
                  class="genre-fill me"
                  :style="{ width: genrePercent(genre, compareData.me.genres) + '%' }"
                ></div>
              </div>
              <span class="genre-percent">{{ genrePercent(genre, compareData.me.genres) }}%</span>
            </div>
            <div v-if="!compareData.me.genres.length" class="genre-empty">暂无数据</div>
          </div>

          <div class="genre-column">
            <h4>对方的类型分布</h4>
            <div
              v-for="genre in compareData.friend.genres"
              :key="`friend-${genre.name}`"
              class="genre-line"
            >
              <span class="genre-name">{{ genre.name }}</span>
              <div class="genre-track">
                <div
                  class="genre-fill friend"
                  :style="{ width: genrePercent(genre, compareData.friend.genres) + '%' }"
                ></div>
              </div>
              <span class="genre-percent">{{ genrePercent(genre, compareData.friend.genres) }}%</span>
            </div>
            <div v-if="!compareData.friend.genres.length" class="genre-empty">暂无数据</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="compareVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'
import { useUserStore } from '@/store/user'
import { usePreferencesStore } from '@/store/preferences'
import { formatDate as formatPrefDate } from '@/utils/date'

const userStore = useUserStore()
const preferencesStore = usePreferencesStore()

const activeTab = ref('friends')

const inviteCode = ref('')
const showInviteDialog = ref(false)
const inviteCodeInput = ref('')
const inviteMessage = ref('')
const sendingInvite = ref(false)

const friends = ref([])
const friendsLoading = ref(false)
const failedCovers = ref({})

const requests = ref([])
const requestDirection = ref('incoming')
const requestsLoading = ref(false)
const handlingId = ref('')

const shares = ref([])
const shareScope = ref('mine')
const sharesLoading = ref(false)
const failedShareCovers = ref({})

const compareVisible = ref(false)
const compareLoading = ref(false)
const compareError = ref('')
const compareData = ref(null)
const compareFriend = ref(null)

const compareMetrics = [
  { key: 'totalGames', label: '游戏数', format: (value) => `${value ?? 0}` },
  { key: 'totalHours', label: '总时长(h)', format: (value) => `${value ?? 0}` },
  { key: 'achievements', label: '成就数', format: (value) => `${value ?? 0}` },
  { key: 'completedGames', label: '通关数', format: (value) => `${value ?? 0}` }
]

const currentUserId = computed(() => userStore.userInfo?.id)

// 日期统一走偏好 dateFormat（服务端时间串为 yyyy-MM-dd HH:mm:ss）
function formatDate(value) {
  return formatPrefDate(value, preferencesStore.state.dateFormat)
}

async function loadInviteCode() {
  try {
    const data = await api.get('/social/invite-code')
    inviteCode.value = data?.inviteCode || ''
  } catch (e) {
    inviteCode.value = ''
  }
}

async function loadFriends() {
  friendsLoading.value = true
  try {
    const list = await api.get('/social/friends')
    friends.value = Array.isArray(list) ? list : []
    failedCovers.value = {}
  } catch (e) {
    // 错误提示由 api.js 统一处理，保留旧列表
  } finally {
    friendsLoading.value = false
  }
}

async function loadRequests() {
  requestsLoading.value = true
  try {
    const list = await api.get('/social/friend-requests', {
      params: { direction: requestDirection.value }
    })
    requests.value = Array.isArray(list) ? list : []
  } catch (e) {
    requests.value = []
  } finally {
    requestsLoading.value = false
  }
}

async function loadShares() {
  sharesLoading.value = true
  try {
    const list = await api.get('/social/shares', { params: { scope: shareScope.value } })
    shares.value = Array.isArray(list) ? list : []
    failedShareCovers.value = {}
  } catch (e) {
    shares.value = []
  } finally {
    sharesLoading.value = false
  }
}

function isCoverFailed(friendId, gameId) {
  return !!failedCovers.value[`${friendId}-${gameId}`]
}

function markCoverFailed(friendId, gameId) {
  failedCovers.value[`${friendId}-${gameId}`] = true
}

function markShareCoverFailed(shareId) {
  failedShareCovers.value[shareId] = true
}

function statusTagType(status) {
  if (status === 'accepted') return 'success'
  if (status === 'rejected') return 'info'
  return 'warning'
}

function requestStatusText(req) {
  if (req.status === 'accepted') return '已接受'
  if (req.status === 'rejected') return '已拒绝'
  return requestDirection.value === 'outgoing' ? '等待确认' : '待处理'
}

function typeLabel(type) {
  const map = {
    year_report: '年度报告',
    achievement: '成就',
    friend_invite: '好友邀请'
  }
  return map[type] || '分享'
}

function visibilityLabel(visibility) {
  const map = {
    public: '公开',
    friends: '好友可见',
    private: '仅自己'
  }
  return map[visibility] || '好友可见'
}

function canDeleteShare(share) {
  const authorId = Number(share?.author?.id)
  const meId = Number(currentUserId.value)
  return Number.isFinite(authorId) && Number.isFinite(meId) && authorId === meId
}

function openInviteDialog() {
  inviteCodeInput.value = ''
  inviteMessage.value = ''
  showInviteDialog.value = true
}

async function submitInvite() {
  const code = inviteCodeInput.value.trim()
  if (!code) {
    ElMessage.warning('请输入好友邀请码')
    return
  }
  if (inviteMessage.value.length > 120) {
    ElMessage.error('附言不能超过 120 字')
    return
  }
  sendingInvite.value = true
  try {
    const response = await api.post('/social/friend-requests', {
      inviteCode: code,
      message: inviteMessage.value.trim()
    })
    // 以服务端返回的真实状态为准：只有 pending 才算"请求已发送"
    const status = response?.status
    if (status === 'pending') {
      ElMessage.success('好友请求已发送')
      showInviteDialog.value = false
      inviteCodeInput.value = ''
      inviteMessage.value = ''
      requestDirection.value = 'outgoing'
      await Promise.all([loadRequests(), loadFriends()])
    } else if (status === 'accepted') {
      // 删除好友后再次发起、或后端状态异常时会出现 accepted，不能谎报发送成功
      ElMessage.warning('对方已是好友或请求状态异常，请刷新')
      await Promise.all([loadRequests(), loadFriends()])
    } else {
      ElMessage.warning(response?.message || '请求状态异常，请刷新后重试')
    }
  } catch (e) {
    // 400 文案（邀请码无效/不能添加自己/已是好友/请求已发送）由 api.js 提示，
    // 失败不关闭弹窗、不清空输入，便于用户修正后重试
  } finally {
    sendingInvite.value = false
  }
}

async function copyInviteCode() {
  if (!inviteCode.value) return
  const text = `GS-${inviteCode.value}`
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const input = document.createElement('textarea')
      input.value = text
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    ElMessage.success('邀请码已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

function handleFriendCommand(command, friend) {
  if (command === 'compare') {
    openCompare(friend)
  } else if (command === 'remove') {
    deleteFriend(friend)
  }
}

async function deleteFriend(friend) {
  try {
    await ElMessageBox.confirm(
      `确定删除好友 ${friend.username} 吗？删除后双方好友关系将解除。`,
      '删除好友',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await api.delete(`/social/friends/${friend.id}`)
    ElMessage.success('已删除好友')
    await loadFriends()
  } catch (e) {
    // 后端 message 由拦截器展示
  }
}

async function handleRequest(req, action) {
  if (handlingId.value) return
  handlingId.value = req.id
  try {
    await api.put(`/social/friend-requests/${req.id}`, { action })
    ElMessage.success(action === 'accept' ? '已接受好友请求' : '已拒绝好友请求')
    await loadRequests()
    if (action === 'accept') {
      await loadFriends()
    }
  } catch (e) {
    // 400/403/404 文案由拦截器展示
  } finally {
    handlingId.value = ''
  }
}

function normalizeCompareSide(side) {
  return {
    id: side?.id ?? null,
    username: side?.username || '未知用户',
    avatar: side?.avatar || '',
    totalGames: side?.totalGames ?? 0,
    totalHours: side?.totalHours ?? 0,
    achievements: side?.achievements ?? 0,
    completedGames: side?.completedGames ?? 0,
    genres: (Array.isArray(side?.genres) ? side.genres : []).map((genre) => ({
      name: genre?.name || '未知类型',
      hours: Number.isFinite(Number(genre?.hours)) ? Number(genre.hours) : 0,
      percent: Number.isFinite(Number(genre?.percent)) ? Number(genre.percent) : null
    }))
  }
}

async function openCompare(friend) {
  compareFriend.value = friend
  compareVisible.value = true
  compareLoading.value = true
  compareError.value = ''
  compareData.value = null
  try {
    const data = await api.get('/social/compare', { params: { with: friend.id } })
    compareData.value = {
      me: normalizeCompareSide(data?.me),
      friend: normalizeCompareSide(data?.friend)
    }
  } catch (e) {
    // 403 隐私文案优先取后端 message（api.js 已改为优先展示 message）
    compareError.value = e?.response?.data?.message || '对方设置了隐私保护，无法对比'
  } finally {
    compareLoading.value = false
  }
}

// 百分比优先后端 percent，缺失时按 hours 现场计算（不写死任何数值）
function genrePercent(genre, list) {
  if (Number.isFinite(genre.percent)) {
    return Math.round(genre.percent)
  }
  const total = (list || []).reduce((sum, item) => sum + (Number(item.hours) || 0), 0)
  if (!total) return 0
  return Math.round(((Number(genre.hours) || 0) / total) * 100)
}

async function deleteShare(share) {
  try {
    await ElMessageBox.confirm(`确定删除分享记录「${share.title}」吗？`, '删除分享', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await api.delete(`/social/shares/${share.id}`)
    ElMessage.success('已删除')
    await loadShares()
  } catch (e) {
    // 404 文案由拦截器展示
  }
}

onMounted(() => {
  loadInviteCode()
  loadFriends()
  loadRequests()
  loadShares()
})
</script>

<style lang="scss" scoped>
.social-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;

  h2 {
    font-size: 24px;
    font-weight: 600;
    color: $text-primary;
  }
}

.header-title {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.invite-code-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: $radius-md;
  background: $bg-card;
  border: 1px solid $border-color;

  .invite-label {
    font-size: 12px;
    color: $text-muted;
  }

  .invite-code {
    font-family: monospace;
    font-size: 14px;
    color: $primary-color;
    letter-spacing: 1px;
  }
}

.tab-panel {
  padding-top: 4px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.tab-empty {
  padding: 24px 0;
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.friend-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  transition: all $transition-normal;

  &:hover {
    border-color: $border-light;
    transform: translateY(-2px);
  }
}

.friend-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.friend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;

  .friend-name {
    font-size: 15px;
    font-weight: 500;
    color: $text-primary;
  }

  .friend-level {
    font-size: 12px;
    color: $primary-color;
  }
}

.menu-btn {
  color: $text-muted;

  &:hover {
    color: $text-primary;
  }
}

.friend-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  font-size: 12px;

  .meta-label {
    color: $text-muted;
  }

  .meta-value {
    color: $text-secondary;
  }
}

.friend-stats {
  display: flex;
  justify-content: space-around;
  padding: 14px 0;
  border-top: 1px solid $border-color;
  border-bottom: 1px solid $border-color;
  margin-bottom: 14px;

  .stat {
    text-align: center;

    .value {
      display: block;
      font-size: 18px;
      font-weight: 600;
      color: $text-primary;
    }

    .label {
      font-size: 12px;
      color: $text-secondary;
    }
  }
}

.recent-games {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  .label {
    flex-shrink: 0;
    font-size: 12px;
    color: $text-muted;
  }

  .game-icons {
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 36px;

    img {
      width: 36px;
      height: 36px;
      border-radius: 6px;
      object-fit: cover;
    }
  }

  .recent-empty {
    font-size: 12px;
    color: $text-muted;
  }
}

.game-cover-placeholder {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: $text-secondary;
  background: linear-gradient(135deg, rgba($primary-color, 0.35), rgba($secondary-color, 0.25));
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-md;
}

.request-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.request-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .request-name {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
  }
}

.request-message {
  font-size: 13px;
  color: $text-secondary;
  word-break: break-all;
}

.request-time {
  font-size: 12px;
  color: $text-muted;
}

.request-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.shares-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.share-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  overflow: hidden;
  transition: all $transition-normal;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
  }
}

.share-cover {
  height: 140px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.share-cover-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.65);
  background: linear-gradient(135deg, rgba($primary-color, 0.55), rgba($secondary-color, 0.4));
}

.share-content {
  padding: 16px;

  h4 {
    margin-top: 10px;
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 8px;
  }

  p {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: 12px;
    min-height: 38px;
    word-break: break-all;
  }
}

.share-badges {
  display: flex;
  gap: 6px;
}

.share-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  .share-author {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $text-secondary;
  }

  .share-date {
    font-size: 12px;
    color: $text-muted;
  }

  .delete-btn {
    margin-left: auto;
    color: $danger-color;
  }
}

.invite-hint {
  margin-top: 4px;
  font-size: 13px;
  color: $text-secondary;

  strong {
    color: $primary-color;
    font-family: monospace;
  }
}

.compare-loading {
  padding: 40px 0;
  text-align: center;
  color: $text-muted;
}

.compare-error {
  padding: 32px 0;
  text-align: center;
  color: $warning-color;

  p {
    margin-top: 10px;
    font-size: 14px;
  }
}

.compare-name-row {
  display: grid;
  grid-template-columns: 1fr 60px 1fr;
  align-items: center;
  margin-bottom: 20px;

  .side-name {
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }

  .vs {
    text-align: center;
    font-size: 18px;
    font-weight: 700;
    color: $primary-color;
  }
}

.compare-metrics {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.compare-metric {
  display: grid;
  grid-template-columns: 1fr 120px 1fr;
  align-items: center;
  padding: 10px 16px;
  border-radius: $radius-md;
  background: $bg-darker;

  .metric-me {
    text-align: left;
    font-size: 15px;
    font-weight: 600;
    color: $primary-color;
  }

  .metric-friend {
    text-align: right;
    font-size: 15px;
    font-weight: 600;
    color: $secondary-color;
  }

  .metric-label {
    text-align: center;
    font-size: 12px;
    color: $text-muted;
  }
}

.compare-genres {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.genre-column {
  h4 {
    font-size: 13px;
    font-weight: 600;
    color: $text-secondary;
    margin-bottom: 12px;
  }
}

.genre-line {
  display: grid;
  grid-template-columns: 64px 1fr 46px;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;

  .genre-name {
    font-size: 12px;
    color: $text-secondary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .genre-percent {
    text-align: right;
    font-size: 12px;
    color: $text-muted;
  }
}

.genre-track {
  height: 8px;
  border-radius: 4px;
  overflow: hidden;
  background: $bg-darker;

  .genre-fill {
    height: 100%;
    border-radius: 4px;

    &.me {
      background: $primary-color;
    }

    &.friend {
      background: $secondary-color;
    }
  }
}

.genre-empty {
  font-size: 12px;
  color: $text-muted;
}

@media (max-width: 640px) {
  .compare-genres {
    grid-template-columns: 1fr;
  }
}
</style>