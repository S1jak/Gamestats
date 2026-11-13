<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    width="760px"
    class="gs-dialog account-settings-dialog"
    align-center
    :show-close="true"
  >
    <template #header>
      <div class="dialog-header">
        <el-icon><Setting /></el-icon>
        <span>账号设置</span>
      </div>
    </template>

    <div class="settings-container">
      <!-- 左侧菜单（Steam 风格：垂直导航 + 当前项指示条） -->
      <aside class="settings-aside">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="aside-item"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
        </div>
      </aside>

      <!-- 右侧内容区 -->
      <main class="settings-main">
        <!-- ============== 资料 ============== -->
        <section v-show="activeTab === 'profile'">
          <h3 class="section-title">个人资料</h3>
          <p class="section-desc">这些信息会显示在你的个人主页上，其他玩家可以查看。</p>

          <div class="avatar-row">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleAvatarChange"
              accept="image/*"
            >
              <el-avatar :size="86" :src="form.avatar">
                {{ form.username?.charAt(0) }}
              </el-avatar>
              <div class="upload-hint">点击更换</div>
            </el-upload>
            <div class="avatar-tip">
              支持 JPG/PNG，文件大小不超过 2MB。<br />
              建议使用 200×200 像素以上的正方形图片。
            </div>
          </div>

          <el-form :model="form" label-position="top" class="settings-form">
            <div class="form-row">
              <el-form-item label="用户名">
                <el-input v-model="form.username" placeholder="请输入用户名" maxlength="20" show-word-limit />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="form.gender">
                  <el-radio value="male">男</el-radio>
                  <el-radio value="female">女</el-radio>
                  <el-radio value="other">保密</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>

            <div class="form-row">
              <el-form-item label="生日">
                <el-date-picker
                  v-model="form.birthday"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择生日"
                  style="width: 100%"
                  :disabled-date="disabledBirthday"
                />
              </el-form-item>
              <el-form-item label="地区">
                <el-select v-model="form.region" placeholder="选择地区" filterable style="width: 100%">
                  <el-option v-for="r in regions" :key="r" :label="r" :value="r" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="个性签名">
              <el-input
                v-model="form.bio"
                type="textarea"
                :rows="3"
                placeholder="一句话介绍自己，3-100 字符"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </section>

        <!-- ============== 隐私 ============== -->
        <section v-show="activeTab === 'privacy'">
          <h3 class="section-title">隐私设置</h3>
          <p class="section-desc">控制谁可以查看你的资料、动态以及与你互动。</p>

          <div class="privacy-list">
            <div class="privacy-item">
              <div class="privacy-info">
                <h4>个人资料可见性</h4>
                <p>谁可以查看你的个人资料页与游戏统计</p>
              </div>
              <el-select v-model="form.profileVisibility" style="width: 160px">
                <el-option label="公开" value="public">
                  <span>公开</span>
                  <span class="opt-sub">所有人可见</span>
                </el-option>
                <el-option label="仅好友" value="friends">
                  <span>仅好友</span>
                  <span class="opt-sub">仅互关用户</span>
                </el-option>
                <el-option label="仅自己" value="private">
                  <span>仅自己</span>
                  <span class="opt-sub">完全私密</span>
                </el-option>
              </el-select>
            </div>

            <div class="privacy-item">
              <div class="privacy-info">
                <h4>允许评论</h4>
                <p>其他用户是否可以在你的动态下评论</p>
              </div>
              <el-switch
                v-model="form.allowComments"
                :active-value="1"
                :inactive-value="0"
                active-color="#5b3cc4"
              />
            </div>
          </div>
        </section>

        <!-- ============== 通知 ============== -->
        <section v-show="activeTab === 'notification'">
          <h3 class="section-title">通知设置</h3>
          <p class="section-desc">选择你希望接收的通知方式。</p>

          <div class="privacy-list">
            <div class="privacy-item">
              <div class="privacy-info">
                <h4>站内通知</h4>
                <p>新粉丝、评论回复、成就解锁等站内消息</p>
              </div>
              <el-switch
                v-model="form.siteNotify"
                :active-value="1"
                :inactive-value="0"
                active-color="#5b3cc4"
              />
            </div>

            <div class="privacy-item">
              <div class="privacy-info">
                <h4>邮件通知</h4>
                <p>每周游戏数据周报、年度报告、活动推荐</p>
              </div>
              <el-switch
                v-model="form.emailNotify"
                :active-value="1"
                :inactive-value="0"
                active-color="#5b3cc4"
              />
            </div>
          </div>
        </section>
      </main>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">保存设置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Lock, Bell, User, View, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import api from '@/utils/api'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const saving = ref(false)
const avatarFile = ref(null)
const activeTab = ref('profile')

// markRaw 包裹 icon 组件避免被 reactive 深度代理影响渲染
const tabs = [
  { key: 'profile', label: '个人资料', icon: markRaw(User) },
  { key: 'privacy', label: '隐私设置', icon: markRaw(View) },
  { key: 'notification', label: '通知设置', icon: markRaw(Bell) }
]

const regions = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京', '西安', '重庆', '其他']

// 生日不能选未来日期
const disabledBirthday = (date) => date.getTime() > Date.now()

const form = reactive({
  username: '',
  bio: '',
  avatar: '',
  gender: 'other',
  birthday: null,
  region: '',
  profileVisibility: 'public',
  allowComments: 1,
  emailNotify: 1,
  siteNotify: 1
})

// 打开时从 store 填充；切换 tab 不会重新加载
watch(() => props.modelValue, (val) => {
  if (val) {
    avatarFile.value = null
    const u = userStore.userInfo || {}
    Object.assign(form, {
      username: u.username || '',
      bio: u.bio || '',
      avatar: u.avatar || '',
      gender: u.gender || 'other',
      birthday: u.birthday || null,
      region: u.region || '',
      profileVisibility: u.profileVisibility || 'public',
      allowComments: u.allowComments ?? 1,
      emailNotify: u.emailNotify ?? 1,
      siteNotify: u.siteNotify ?? 1
    })
    activeTab.value = 'profile'
  }
})

function handleAvatarChange(file) {
  const raw = file.raw
  if (!raw || !raw.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  if (raw.size > 2 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 2MB')
    return
  }
  avatarFile.value = raw
  const reader = new FileReader()
  reader.onload = (e) => { form.avatar = e.target.result }
  reader.readAsDataURL(raw)
}

function handleClose() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  if (!form.username?.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  saving.value = true
  try {
    let avatarUrl = form.avatar
    if (avatarFile.value) {
      const fd = new FormData()
      fd.append('file', avatarFile.value)
      const res = await api.post('/upload/avatar', fd)
      avatarUrl = res.url
    }
    // birthday 字段空字符串会被后端 DTO 转 null，避免日期格式异常
    const payload = {
      ...form,
      avatar: avatarUrl,
      birthday: form.birthday || null
    }
    await userStore.updateProfile(payload)
    avatarFile.value = null
    ElMessage.success('账号设置已保存')
    handleClose()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
// 头部图标对齐
.dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}

.settings-container {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 0;
  min-height: 480px;
}

// 左侧菜单
.settings-aside {
  background: rgba(10, 10, 26, 0.5);
  border-right: 1px solid rgba(99, 102, 241, 0.15);
  padding: 12px 0;
  border-radius: 12px 0 0 12px;

  .aside-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 20px;
    color: #a8a8d4;
    cursor: pointer;
    font-size: 14px;
    border-left: 3px solid transparent;
    transition: all 0.25s ease;

    .el-icon {
      font-size: 16px;
    }

    &:hover {
      background: rgba(99, 102, 241, 0.08);
      color: #d4d4e8;
    }

    &.active {
      background: linear-gradient(90deg, rgba(91, 60, 196, 0.25) 0%, rgba(99, 102, 241, 0.05) 100%);
      color: #ffffff;
      border-left-color: #5b3cc4;
      box-shadow: inset 0 0 12px rgba(91, 60, 196, 0.15);

      .el-icon { color: #818cf8; }
    }
  }
}

.settings-main {
  padding: 8px 28px 0 28px;
  overflow-y: auto;
  max-height: 540px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 6px;
}

.section-desc {
  font-size: 13px;
  color: #71717a;
  margin-bottom: 24px;
}

// 头像行
.avatar-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px;
  background: rgba(10, 10, 26, 0.4);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 12px;
  margin-bottom: 24px;
}

.avatar-tip {
  font-size: 12px;
  color: #71717a;
  line-height: 1.6;
}

.avatar-uploader {
  :deep(.el-upload) {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .el-avatar {
      transition: all 0.3s ease;
      border: 2px solid transparent;
    }

    &:hover .el-avatar {
      box-shadow: 0 0 0 2px #5b3cc4, 0 0 20px rgba(91, 60, 196, 0.4);
      border-color: #5b3cc4;
    }
  }

  .upload-hint {
    font-size: 12px;
    color: #818cf8;
  }
}

.settings-form {
  :deep(.el-form-item__label) {
    color: #a8a8d4;
    font-weight: 500;
  }
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

// 隐私/通知列表
.privacy-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.privacy-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  background: rgba(10, 10, 26, 0.4);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 12px;
  transition: all 0.25s ease;

  &:hover {
    border-color: rgba(99, 102, 241, 0.35);
    background: rgba(10, 10, 26, 0.55);
  }

  .privacy-info {
    flex: 1;

    h4 {
      font-size: 14px;
      font-weight: 600;
      color: #ffffff;
      margin-bottom: 4px;
    }

    p {
      font-size: 12px;
      color: #71717a;
      line-height: 1.5;
    }
  }
}

.opt-sub {
  margin-left: 8px;
  font-size: 11px;
  color: #71717a;
}
</style>

<!-- teleport 到 body 的下拉箭头配色（el-select） -->
<style lang="scss">
.account-settings-dialog {
  .el-select__wrapper {
    background-color: rgba(10, 10, 26, 0.6) !important;
    box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.25) inset !important;
    transition: all 0.3s ease;
  }
  .el-select__placeholder { color: #71717a; }
  .el-select__selected-item { color: #ffffff; }
  .el-input__inner { color: #ffffff; }
}
</style>
