<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    width="720px"
    class="gs-dialog preference-dialog"
    align-center
  >
    <template #header>
      <div class="dialog-header">
        <el-icon><Brush /></el-icon>
        <span>偏好设置</span>
      </div>
    </template>

    <div class="pref-container">
      <!-- 左侧菜单 -->
      <aside class="pref-aside">
        <div
          v-for="t in tabs"
          :key="t.key"
          class="aside-item"
          :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key"
        >
          <el-icon><component :is="t.icon" /></el-icon>
          <span>{{ t.label }}</span>
        </div>
      </aside>

      <!-- 右侧内容 -->
      <main class="pref-main">
        <!-- 外观 -->
        <section v-show="activeTab === 'appearance'">
          <h3 class="section-title">外观</h3>
          <p class="section-desc">主题与界面风格，决定整个应用的颜色基调。</p>

          <div class="theme-grid">
            <div
              v-for="t in themes"
              :key="t.value"
              class="theme-card"
              :class="{ active: prefs.theme === t.value }"
              @click="prefs.theme = t.value"
            >
              <div class="theme-preview" :style="{ background: t.preview }">
                <div class="preview-bar" :style="{ background: t.accent }"></div>
              </div>
              <div class="theme-name">
                <el-icon v-if="prefs.theme === t.value" class="check-icon"><Check /></el-icon>
                {{ t.label }}
              </div>
              <div class="theme-desc">{{ t.desc }}</div>
            </div>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>动效</h4>
              <p>页面切换、卡片悬停等微动效（关闭可降低性能占用）</p>
            </div>
            <el-switch v-model="prefs.motion" active-color="#5b3cc4" />
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>侧边栏</h4>
              <p>默认折叠或展开主导航栏</p>
            </div>
            <el-switch v-model="prefs.sidebarCollapsed" active-color="#5b3cc4" />
          </div>
        </section>

        <!-- 语言 -->
        <section v-show="activeTab === 'language'">
          <h3 class="section-title">语言与地区</h3>
          <p class="section-desc">选择界面显示语言与数据格式。</p>

          <div class="lang-grid">
            <div
              v-for="l in languages"
              :key="l.value"
              class="lang-card"
              :class="{ active: prefs.language === l.value }"
              @click="prefs.language = l.value"
            >
              <div class="lang-flag">{{ l.flag }}</div>
              <div class="lang-name">{{ l.label }}</div>
              <div class="lang-sub">{{ l.sub }}</div>
            </div>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>时区</h4>
              <p>影响数据统计的时间范围</p>
            </div>
            <el-select v-model="prefs.timezone" style="width: 180px">
              <el-option label="UTC+8 北京时间" value="Asia/Shanghai" />
              <el-option label="UTC+0 伦敦" value="Europe/London" />
              <el-option label="UTC-5 纽约" value="America/New_York" />
            </el-select>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>日期格式</h4>
              <p>列表与图表中日期的显示方式</p>
            </div>
            <el-radio-group v-model="prefs.dateFormat">
              <el-radio value="YYYY-MM-DD">2026-07-02</el-radio>
              <el-radio value="MM/DD/YYYY">07/02/2026</el-radio>
            </el-radio-group>
          </div>
        </section>

        <!-- 数据 -->
        <section v-show="activeTab === 'data'">
          <h3 class="section-title">数据可视化</h3>
          <p class="section-desc">数据中心页面的默认展示方式。</p>

          <div class="setting-row">
            <div class="setting-info">
              <h4>默认图表</h4>
              <p>数据中心的默认展示图表类型</p>
            </div>
            <el-radio-group v-model="prefs.defaultChart">
              <el-radio-button value="line">折线</el-radio-button>
              <el-radio-button value="bar">柱状</el-radio-button>
              <el-radio-button value="area">面积</el-radio-button>
            </el-radio-group>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>默认时间范围</h4>
              <p>趋势图的默认统计区间</p>
            </div>
            <el-radio-group v-model="prefs.defaultRange">
              <el-radio value="7">近 7 天</el-radio>
              <el-radio value="30">近 30 天</el-radio>
              <el-radio value="90">近 90 天</el-radio>
            </el-radio-group>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>显示小数</h4>
              <p>统计数字是否保留小数位（百分比/时长）</p>
            </div>
            <el-switch v-model="prefs.showDecimal" active-color="#5b3cc4" />
          </div>
        </section>

        <!-- 通知 -->
        <section v-show="activeTab === 'notify'">
          <h3 class="section-title">通知与提醒</h3>
          <p class="section-desc">在应用内与桌面接收的提醒类型。</p>

          <div class="setting-row">
            <div class="setting-info">
              <h4>成就解锁提醒</h4>
              <p>游戏内解锁新成就时弹出应用内通知</p>
            </div>
            <el-switch v-model="prefs.notifyAchievement" active-color="#5b3cc4" />
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>每日签到提醒</h4>
              <p>每天 20:00 提醒打卡记录游戏时长</p>
            </div>
            <el-switch v-model="prefs.notifyDaily" active-color="#5b3cc4" />
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <h4>每周报告</h4>
              <p>每周一发送上周游玩数据汇总</p>
            </div>
            <el-switch v-model="prefs.notifyWeekly" active-color="#5b3cc4" />
          </div>
        </section>
      </main>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button @click="handleReset">恢复默认</el-button>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import { Brush, MagicStick, ChatLineRound, DataLine, Check } from '@element-plus/icons-vue'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const STORAGE_KEY = 'gs_preferences'
const activeTab = ref('appearance')

const tabs = [
  { key: 'appearance', label: '外观', icon: markRaw(MagicStick) },
  { key: 'language', label: '语言', icon: markRaw(ChatLineRound) },
  { key: 'data', label: '数据', icon: markRaw(DataLine) },
  { key: 'notify', label: '通知', icon: markRaw(Brush) }
]

const themes = [
  { value: 'purple', label: '紫蓝渐变', desc: '默认主题 · 与登录页一致', preview: 'linear-gradient(135deg, #5b3cc4 0%, #3a6ee0 100%)', accent: 'rgba(255,255,255,0.85)' },
  { value: 'dark', label: '暗夜深邃', desc: '纯黑底色，OLED 友好', preview: 'linear-gradient(135deg, #1a1a2e 0%, #0a0a1a 100%)', accent: 'rgba(255,255,255,0.6)' },
  { value: 'light', label: '明亮清新', desc: '清爽白底，适合白天', preview: 'linear-gradient(135deg, #ffffff 0%, #e4e4e7 100%)', accent: 'rgba(91,60,196,0.5)' }
]

const languages = [
  { value: 'zh-CN', label: '简体中文', sub: '中国大陆', flag: '🇨🇳' },
  { value: 'zh-TW', label: '繁體中文', sub: '台灣 / 香港', flag: '🇭🇰' },
  { value: 'en-US', label: 'English', sub: 'United States', flag: '🇺🇸' },
  { value: 'ja-JP', label: '日本語', sub: '日本', flag: '🇯🇵' }
]

const defaults = {
  theme: 'purple',
  language: 'zh-CN',
  motion: true,
  sidebarCollapsed: false,
  timezone: 'Asia/Shanghai',
  dateFormat: 'YYYY-MM-DD',
  defaultChart: 'line',
  defaultRange: '30',
  showDecimal: true,
  notifyAchievement: true,
  notifyDaily: true,
  notifyWeekly: false
}

const prefs = reactive({ ...defaults })

// 打开时加载持久化偏好，关闭时下次打开再读（不实时回滚）
watch(() => props.modelValue, (val) => {
  if (val) {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
      if (saved) Object.assign(prefs, saved)
      else Object.assign(prefs, defaults)
    } catch {
      Object.assign(prefs, defaults)
    }
    activeTab.value = 'appearance'
  }
})

function handleClose() {
  emit('update:modelValue', false)
}

function handleReset() {
  Object.assign(prefs, defaults)
  ElMessage.info('已恢复默认偏好，点击保存生效')
}

function handleSubmit() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs))
  ElMessage.success('偏好已保存')
  handleClose()
}
</script>

<style lang="scss" scoped>
.dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}

.pref-container {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 0;
  min-height: 460px;
}

.pref-aside {
  background: rgba(10, 10, 26, 0.5);
  border-right: 1px solid rgba(99, 102, 241, 0.15);
  padding: 12px 0;
  border-radius: 12px 0 0 12px;

  .aside-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 18px;
    color: #a8a8d4;
    cursor: pointer;
    font-size: 14px;
    border-left: 3px solid transparent;
    transition: all 0.25s ease;

    .el-icon { font-size: 16px; }

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

.pref-main {
  padding: 8px 28px 0;
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

// 主题卡片
.theme-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.theme-card {
  padding: 12px;
  background: rgba(10, 10, 26, 0.4);
  border: 2px solid rgba(99, 102, 241, 0.15);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(99, 102, 241, 0.4);
    transform: translateY(-2px);
  }

  &.active {
    border-color: #5b3cc4;
    box-shadow: 0 0 20px rgba(91, 60, 196, 0.3);

    .check-icon { color: #818cf8; }
  }

  .theme-preview {
    height: 64px;
    border-radius: 8px;
    margin-bottom: 10px;
    display: flex;
    align-items: flex-end;
    padding: 8px;

    .preview-bar {
      width: 100%;
      height: 6px;
      border-radius: 3px;
    }
  }

  .theme-name {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    font-weight: 600;
    color: #ffffff;
    margin-bottom: 2px;
  }

  .check-icon { font-size: 14px; }

  .theme-desc {
    font-size: 12px;
    color: #71717a;
  }
}

// 语言卡片
.lang-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.lang-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: rgba(10, 10, 26, 0.4);
  border: 2px solid rgba(99, 102, 241, 0.15);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(99, 102, 241, 0.4);
  }

  &.active {
    border-color: #5b3cc4;
    background: rgba(91, 60, 196, 0.12);
  }

  .lang-flag {
    font-size: 28px;
  }

  .lang-name {
    font-size: 14px;
    font-weight: 600;
    color: #ffffff;
  }

  .lang-sub {
    font-size: 12px;
    color: #71717a;
  }
}

// 设置行
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
  background: rgba(10, 10, 26, 0.4);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 12px;
  margin-bottom: 10px;
  transition: all 0.25s ease;

  &:hover {
    border-color: rgba(99, 102, 241, 0.35);
  }

  .setting-info {
    flex: 1;
    h4 {
      font-size: 14px;
      font-weight: 600;
      color: #ffffff;
      margin-bottom: 2px;
    }
    p {
      font-size: 12px;
      color: #71717a;
      line-height: 1.5;
    }
  }
}
</style>
