<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="avatar-section">
        <el-avatar :size="100" :src="userInfo.avatar">
          {{ userInfo.username?.charAt(0) }}
        </el-avatar>
        <div class="user-info">
          <h1>{{ userInfo.username }}</h1>
          <div class="user-badges">
            <span class="badge level">Lv.{{ userInfo.level || 1 }}</span>
            <span class="badge type">{{ userTypeLabel }}</span>
          </div>
          <p class="join-date">{{ formatJoinDate(userInfo.joinDate) }} 加入</p>
        </div>
      </div>
      <el-button type="primary" @click="showEditDialog = true">
        <el-icon><Edit /></el-icon>
        编辑资料
      </el-button>
    </div>
    
    <div class="profile-stats">
      <div class="stat-item">
        <span class="stat-value">{{ stats.totalGames }}</span>
        <span class="stat-label">游戏</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ formatPlayTime(stats.totalPlayTime) }}</span>
        <span class="stat-label">总时长</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.achievements }}</span>
        <span class="stat-label">成就</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.completedGames }}</span>
        <span class="stat-label">已通关</span>
      </div>
    </div>
    
    <div class="profile-content">
      <div class="content-section">
        <h3>绑定账号</h3>
        <div class="linked-accounts">
          <div
            v-for="platform in platforms"
            :key="platform.key"
            class="account-item"
            :class="{ linked: !!bindingOf(platform.key)?.bound }"
          >
            <div class="account-icon" :class="platform.key">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path :d="platform.icon"/>
              </svg>
            </div>
            <div class="account-info">
              <span class="account-name">{{ platform.name }}</span>
              <span class="account-status">{{ bindingStatus(platform.key) }}</span>
            </div>
            <el-button
              :type="bindingOf(platform.key)?.bound ? 'default' : 'primary'"
              size="small"
              :loading="unbindingPlatform === platform.key"
              :disabled="bindingsLoading || bindingsError"
              @click="handleLink(platform.key)"
            >
              {{ bindingOf(platform.key)?.bound ? '解绑' : '绑定' }}
            </el-button>
          </div>
          <div v-if="bindingsError" class="bindings-error">
            <span>绑定状态加载失败，请重试</span>
            <el-button text size="small" @click="loadBindings">重试</el-button>
          </div>
        </div>
      </div>
      
      <div class="content-section">
        <h3>游戏偏好</h3>
        <div class="genre-preferences">
          <div 
            v-for="genre in genrePreferences" 
            :key="genre.name" 
            class="genre-item"
          >
            <span class="genre-name">{{ genre.name }}</span>
            <el-progress 
              :percentage="genre.percentage" 
              :stroke-width="8"
              :color="genre.color"
            />
            <span class="genre-time">{{ genre.hours }}小时</span>
          </div>
        </div>
      </div>
      
      <div class="content-section">
        <h3>游戏标签</h3>
        <div class="custom-tags">
          <el-tag 
            v-for="tag in customTags" 
            :key="tag" 
            closable
            size="large"
            @close="removeTag(tag)"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-model="newTag"
            placeholder="添加标签"
            size="small"
            class="tag-input"
            @keyup.enter="addTag"
          >
            <template #append>
              <el-button @click="addTag">添加</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>
    
    <el-dialog
      v-model="showEditDialog"
      title="编辑资料"
      width="500px"
      class="edit-profile-dialog"
      align-center
    >
      <el-form :model="editForm" label-width="80px" class="edit-form">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleAvatarChange"
            accept=".jpg,.jpeg,.png,.webp,.gif"
          >
            <el-avatar :size="80" :src="editForm.avatar">
              {{ editForm.username?.charAt(0) }}
            </el-avatar>
            <div class="upload-hint">点击更换头像</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" class="save-btn" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usePreferencesStore } from '@/store/preferences'
import { formatDate as formatPrefDate } from '@/utils/date'
import api from '@/utils/api'

const userStore = useUserStore()
const preferencesStore = usePreferencesStore()
const showEditDialog = ref(false)
const newTag = ref('')
const saving = ref(false)
// 待上传的头像文件：选择后本地 Base64 预览，点"保存"时才走 axios 上传到服务器
const avatarFile = ref(null)

// ===== 真实数据（均来自后端查询）=====
// 用户资料：GET /api/user/profile（userStore.fetchUserInfo）
const userInfo = computed(() => userStore.userInfo || {})

// 游戏统计：GET /api/user/stats（userStore.fetchGameStats）
const stats = computed(() => userStore.gameStats || {})

// 自定义标签：来自 users.custom_tags（UserVO.customTags，JSON 反序列化后的 List）
const customTags = computed(() => userStore.userInfo?.customTags || [])

// ===== 第三方账号绑定：真实接口 GET /api/user/bindings =====
// 固定三平台顺序，未绑定显示"未绑定"，加载失败显示"状态未知"并禁用操作
const PLATFORM_ORDER = ['steam', 'epic', 'discord']

const platforms = [
  {
    key: 'steam',
    name: 'Steam',
    icon: 'M12 2C6.48 2 2 6.48 2 12c0 4.84 3.44 8.87 8 9.8V15H8v-3h2V9.5C10 7.57 11.57 6 13.5 6H16v3h-2c-.55 0-1 .45-1 1v2h3l-.5 3H13v6.95c5.05-.5 9-4.76 9-9.95 0-5.52-4.48-10-10-10z'
  },
  {
    key: 'epic',
    name: 'Epic Games',
    icon: 'M3 3h18v18H3V3zm16 16V5H5v14h14z'
  },
  {
    key: 'discord',
    name: 'Discord',
    icon: 'M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z'
  }
]

const bindings = ref([])
const bindingsLoading = ref(false)
const bindingsError = ref(false)
const unbindingPlatform = ref('')

function bindingOf(platform) {
  return bindings.value.find((item) => item.platform === platform)
}

function bindingStatus(platform) {
  if (bindingsError.value) return '状态未知'
  const item = bindingOf(platform)
  if (!item) return bindingsLoading.value ? '加载中…' : '状态未知'
  return item.bound ? `已绑定 · ${item.boundAt || ''}` : '未绑定'
}

async function loadBindings() {
  bindingsLoading.value = true
  bindingsError.value = false
  try {
    const list = await api.get('/user/bindings')
    // 后端固定返回 3 项，前端仍按顺序兜底，避免缺项导致按钮消失
    bindings.value = PLATFORM_ORDER.map((platform) => {
      const found = (Array.isArray(list) ? list : []).find((item) => item.platform === platform)
      return found || { platform, bound: false, boundAt: null, displayName: null }
    })
  } catch (e) {
    bindingsError.value = true
    bindings.value = []
  } finally {
    bindingsLoading.value = false
  }
}

// 游戏类型偏好：GET /api/dashboard/genre-distribution（含 hours + percent）
const genrePreferences = ref([])
const GENRE_COLORS = ['#6366f1', '#22d3ee', '#f472b6', '#fbbf24', '#22c55e', '#f97316', '#a855f7', '#ef4444']

async function loadGenrePreferences() {
  try {
    const list = await api.get('/dashboard/genre-distribution')
    genrePreferences.value = (list || []).map((g, i) => ({
      name: g.name,
      hours: Math.round(g.hours || 0),
      percentage: Math.round(g.percent || 0),
      color: GENRE_COLORS[i % GENRE_COLORS.length]
    }))
  } catch (e) {
    genrePreferences.value = []
  }
}

// 编辑表单（打开对话框时从真实 userInfo 填充）
const editForm = ref({ username: '', bio: '', avatar: '' })
watch(showEditDialog, (val) => {
  if (val) {
    editForm.value = {
      username: userInfo.value.username || '',
      bio: userInfo.value.bio || '',
      avatar: userInfo.value.avatar || ''
    }
    avatarFile.value = null
  }
})

const userTypeLabel = computed(() => {
  const hours = (stats.value.totalPlayTime || 0) / 60
  if (hours > 3000) return '传奇玩家'
  if (hours > 1000) return '资深玩家'
  if (hours > 300) return '活跃玩家'
  return '新晋玩家'
})

function formatPlayTime(minutes) {
  const hours = Math.floor((minutes || 0) / 60)
  return `${hours}小时`
}

// joinDate 按偏好 dateFormat 渲染（兼容字符串与 Jackson 数组格式）
function formatJoinDate(date) {
  return formatPrefDate(date, preferencesStore.state.dateFormat)
}

// 绑定：PUT 获取后端 400 文案"真实授权暂未开放"（不改本地状态）
// 解绑：二次确认 → DELETE → 成功后重拉列表，失败保留原状态
async function handleLink(platform) {
  if (bindingsError.value || bindingsLoading.value) return
  const binding = bindingOf(platform)
  const name = platforms.find((item) => item.key === platform)?.name || platform

  if (!binding?.bound) {
    try {
      await api.put(`/user/bindings/${platform}`)
    } catch (e) {
      // 400 message（真实授权暂未开放）由 axios 拦截器统一展示
    }
    return
  }

  try {
    await ElMessageBox.confirm(`确定要解绑 ${name} 吗？`, '解绑确认', {
      confirmButtonText: '解绑',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }

  unbindingPlatform.value = platform
  try {
    await api.delete(`/user/bindings/${platform}`)
    ElMessage.success('解绑成功')
    await loadBindings()
  } catch (e) {
    // 后端 message（如"该平台尚未绑定"）由拦截器展示，状态保持不变
  } finally {
    unbindingPlatform.value = ''
  }
}

// 标签增删：构造新数组后调用 PUT /api/user/profile 持久化，
// 后端整体覆盖 users.custom_tags，成功后 userStore.userInfo 刷新，computed 自动更新
async function addTag() {
  const tag = newTag.value.trim()
  if (!tag || customTags.value.includes(tag)) return
  const newTags = [...customTags.value, tag]
  newTag.value = ''
  try {
    await userStore.updateProfile({ customTags: newTags })
  } catch (e) {
    ElMessage.error('标签保存失败')
  }
}

async function removeTag(tag) {
  const newTags = customTags.value.filter(t => t !== tag)
  try {
    await userStore.updateProfile({ customTags: newTags })
  } catch (e) {
    ElMessage.error('标签删除失败')
  }
}

// 头像本地校验：与后端限制保持一致（JPG/JPEG/PNG/WEBP/GIF、≤2MB、空文件拒绝）
const AVATAR_MAX_SIZE = 2 * 1024 * 1024
const AVATAR_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const AVATAR_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'gif']

function validateAvatarFile(raw) {
  if (!raw || !raw.size) {
    ElMessage.error('上传文件不能为空')
    return false
  }
  if (raw.size > AVATAR_MAX_SIZE) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  const ext = (raw.name || '').split('.').pop()?.toLowerCase() || ''
  const mimeOk = !raw.type || AVATAR_MIME_TYPES.includes(raw.type)
  if (!AVATAR_EXTENSIONS.includes(ext) || !mimeOk) {
    ElMessage.error('不支持的文件格式，仅允许 jpg/jpeg/png/webp/gif')
    return false
  }
  return true
}

// 选择头像后本地即时预览（Base64），保存时才上传到服务器
function handleAvatarChange(file) {
  const raw = file.raw
  if (!validateAvatarFile(raw)) return
  avatarFile.value = raw
  const reader = new FileReader()
  reader.onload = (e) => {
    editForm.value.avatar = e.target.result
  }
  reader.readAsDataURL(raw)
}

async function saveProfile() {
  saving.value = true
  try {
    let avatarUrl = editForm.value.avatar
    // 若用户选了新头像文件，先走 axios 上传拿 URL（拦截器自动携带 JWT），再更新资料
    if (avatarFile.value) {
      const formData = new FormData()
      formData.append('file', avatarFile.value)
      // 不要手动设 Content-Type：axios 传 FormData 时会自动加
      // multipart/form-data; boundary=xxx，手动设会丢失 boundary → 后端无法解析 → 400
      const res = await api.post('/upload/avatar', formData)
      avatarUrl = res.url
    }
    await userStore.updateProfile({
      username: editForm.value.username,
      bio: editForm.value.bio,
      avatar: avatarUrl
    })
    avatarFile.value = null
    showEditDialog.value = false
    ElMessage.success('资料已更新')
  } catch (e) {
    ElMessage.error('资料保存失败')
  } finally {
    saving.value = false
  }
}

// 页面挂载时拉取真实数据，失败时不影响渲染（computed 兜底为空对象/空数组）
onMounted(() => {
  userStore.fetchUserInfo().catch(() => {})
  userStore.fetchGameStats().catch(() => {})
  loadGenrePreferences()
  loadBindings()
})
</script>

<style lang="scss" scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid $border-color;
}

.avatar-section {
  display: flex;
  gap: 20px;
  
  .user-info {
    h1 {
      font-size: 28px;
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 8px;
    }
  }
  
  .user-badges {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
    
    .badge {
      padding: 4px 12px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;
      
      &.level {
        background: linear-gradient(135deg, $primary-color, $secondary-color);
        color: white;
      }
      
      &.type {
        background: rgba($primary-color, 0.1);
        color: $primary-color;
      }
    }
  }
  
  .join-date {
    font-size: 13px;
    color: $text-muted;
  }
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-item {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  text-align: center;
  
  .stat-value {
    display: block;
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 4px;
  }
  
  .stat-label {
    font-size: 13px;
    color: $text-secondary;
  }
}

.content-section {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 24px;
  margin-bottom: 20px;
  
  h3 {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 20px;
  }
}

.linked-accounts {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bindings-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: $radius-md;
  background: rgba($danger-color, 0.08);
  border: 1px solid rgba($danger-color, 0.25);
  font-size: 12px;
  color: $danger-color;
}

.account-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: $bg-darker;
  border-radius: $radius-md;
  transition: all $transition-fast;
  
  &:hover {
    background: $bg-card-hover;
  }
  
  &.linked {
    .account-icon {
      opacity: 1;
    }
  }
}

.account-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
  
  svg {
    width: 24px;
    height: 24px;
    color: white;
  }
  
  &.steam { background: #1b2838; }
  &.epic { background: #0078f2; }
  &.discord { background: #5865f2; }
}

.account-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .account-name {
    font-size: 15px;
    font-weight: 500;
    color: $text-primary;
  }
  
  .account-status {
    font-size: 12px;
    color: $text-muted;
  }
}

.genre-preferences {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.genre-item {
  display: grid;
  grid-template-columns: 80px 1fr 80px;
  align-items: center;
  gap: 16px;
  
  .genre-name {
    font-size: 14px;
    color: $text-primary;
  }
  
  .genre-time {
    font-size: 13px;
    color: $text-secondary;
    text-align: right;
  }
}

.custom-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  
  .el-tag {
    background: rgba($primary-color, 0.1);
    border-color: rgba($primary-color, 0.3);
    color: $primary-color;
  }
  
  .tag-input {
    width: 180px;
  }
}

.avatar-uploader {
  display: flex;
  align-items: center;
  gap: 16px;

  .upload-hint {
    font-size: 13px;
    color: $text-secondary;
  }
}
</style>

<!-- ============================================================
     编辑资料弹窗：紫蓝渐变主题
     el-dialog 默认 teleport 到 body，scoped 样式无法穿透，
     故用独立非 scoped 块 + .edit-profile-dialog 前缀限定作用域，
     避免污染其他弹窗。配色与登录页（$primary-color / $secondary-color）一致。
     ============================================================ -->
<style lang="scss">
.edit-profile-dialog {
  border-radius: 16px !important;
  overflow: hidden;
  border: 1px solid rgba(99, 102, 241, 0.3) !important;
  box-shadow: 0 20px 60px rgba(91, 60, 196, 0.35), 0 8px 32px rgba(0, 0, 0, 0.5) !important;
  background: #1a1a2e !important;

  // 头部：紫蓝渐变 + 白色标题 + 光晕
  .el-dialog__header {
    background: linear-gradient(135deg, #5b3cc4 0%, #3a6ee0 100%);
    box-shadow: 0 4px 20px rgba(91, 60, 196, 0.35);
    padding: 18px 24px;
    margin-right: 0;

    .el-dialog__title {
      color: #ffffff;
      font-size: 17px;
      font-weight: 600;
      letter-spacing: 1px;
    }

    .el-dialog__headerbtn {
      top: 18px;
      right: 20px;

      .el-dialog__close {
        color: rgba(255, 255, 255, 0.88);
        font-size: 18px;
        transition: all 0.3s ease;

        &:hover {
          color: #ffffff;
          transform: scale(1.15);
          text-shadow: 0 0 8px rgba(255, 255, 255, 0.6);
        }
      }
    }
  }

  // 主体：淡紫蓝渐变磨砂，非纯白
  .el-dialog__body {
    background: linear-gradient(180deg, rgba(91, 60, 196, 0.1) 0%, rgba(58, 110, 224, 0.06) 100%), #1a1a2e;
    padding: 28px 24px;
    color: #e4e4e7;
  }

  // 底部
  .el-dialog__footer {
    background: #1a1a2e;
    border-top: 1px solid rgba(99, 102, 241, 0.15);
    padding: 16px 24px 20px;
  }
}

// 表单项：紫蓝聚焦光晕 + 深色输入框
.edit-profile-dialog .edit-form {
  .el-form-item__label {
    color: #a8a8d4;
    font-weight: 500;
  }

  .el-input__wrapper {
    background-color: rgba(10, 10, 26, 0.6) !important;
    box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.25) inset !important;
    border-radius: 10px !important;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.5) inset !important;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px #5b3cc4 inset, 0 0 12px rgba(91, 60, 196, 0.35) !important;
    }
  }

  .el-input__inner {
    color: #ffffff;
  }

  .el-textarea__inner {
    background-color: rgba(10, 10, 26, 0.6) !important;
    box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.25) inset !important;
    border-radius: 10px !important;
    color: #ffffff;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.5) inset !important;
    }

    &:focus {
      box-shadow: 0 0 0 1px #5b3cc4 inset, 0 0 12px rgba(91, 60, 196, 0.35) !important;
    }
  }
}

// 按钮：保存紫蓝渐变主按钮悬停上浮，取消描边紫蓝
.edit-profile-dialog {
  .save-btn {
    background: linear-gradient(135deg, #5b3cc4 0%, #3a6ee0 100%) !important;
    border: none !important;
    color: #ffffff !important;
    font-weight: 600;
    letter-spacing: 2px;
    border-radius: 10px !important;
    padding: 10px 28px !important;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(91, 60, 196, 0.45) !important;
      opacity: 0.95;
    }

    &.is-loading {
      transform: none;
    }
  }

  .cancel-btn {
    background: transparent !important;
    border: 1px solid rgba(99, 102, 241, 0.5) !important;
    color: #a8a8d4 !important;
    border-radius: 10px !important;
    padding: 10px 28px !important;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(99, 102, 241, 0.12) !important;
      border-color: #5b3cc4 !important;
      color: #ffffff !important;
    }
  }
}

// 头像上传区域悬停反馈
.edit-profile-dialog .avatar-uploader {
  .el-upload {
    display: flex;
    align-items: center;
    gap: 16px;
    cursor: pointer;

    .el-avatar {
      transition: all 0.3s ease;
    }

    &:hover {
      .el-avatar {
        box-shadow: 0 0 0 2px #5b3cc4, 0 0 16px rgba(91, 60, 196, 0.4);
      }

      .upload-hint {
        color: #818cf8;
      }
    }
  }

  .upload-hint {
    font-size: 13px;
    color: #71717a;
    transition: all 0.3s ease;
  }
}
</style>
