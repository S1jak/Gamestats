import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

// localStorage 仅作首屏缓存，服务端 user_preferences 才是唯一真源
export const PREFERENCES_STORAGE_KEY = 'gs_preferences'

// 默认值与后端 user_preferences DDL 默认值保持一致：
// theme=purple / defaultChart=area / defaultRange='30' / dateFormat=YYYY-MM-DD
export const DEFAULT_PREFERENCES = Object.freeze({
  theme: 'purple',
  motion: true,
  sidebarCollapsed: false,
  language: 'zh-CN',
  timezone: 'Asia/Shanghai',
  dateFormat: 'YYYY-MM-DD',
  defaultChart: 'area',
  defaultRange: '30',
  showDecimal: true,
  notifyAchievement: true,
  notifyDaily: true,
  notifyWeekly: false
})

const THEMES = ['purple', 'dark', 'light']
const CHARTS = ['line', 'bar', 'area']
const RANGES = ['7', '30', '90']
const DATE_FORMATS = ['YYYY-MM-DD', 'MM/DD/YYYY']
const BOOLEAN_FIELDS = [
  'motion',
  'sidebarCollapsed',
  'showDecimal',
  'notifyAchievement',
  'notifyDaily',
  'notifyWeekly'
]

function toBoolean(value) {
  return value === true || value === 1 || value === '1' || value === 'true'
}

// 归一化：布尔字段统一 true/false，枚举非法值回落默认，避免界面消费到未知值
function normalize(source) {
  const base = { ...DEFAULT_PREFERENCES }
  if (!source || typeof source !== 'object' || Array.isArray(source)) return base
  const merged = { ...base, ...source }

  BOOLEAN_FIELDS.forEach((key) => {
    merged[key] = toBoolean(merged[key])
  })

  if (!THEMES.includes(merged.theme)) merged.theme = base.theme
  if (!CHARTS.includes(merged.defaultChart)) merged.defaultChart = base.defaultChart
  const range = String(merged.defaultRange)
  merged.defaultRange = RANGES.includes(range) ? range : base.defaultRange
  if (!DATE_FORMATS.includes(merged.dateFormat)) merged.dateFormat = base.dateFormat
  // 本轮语言仅支持 zh-CN（C8：语言禁用，不做 i18n）
  merged.language = 'zh-CN'

  return merged
}

function readCache() {
  try {
    const raw = localStorage.getItem(PREFERENCES_STORAGE_KEY)
    if (!raw) return { ...DEFAULT_PREFERENCES }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('invalid cache')
    }
    return normalize(parsed)
  } catch (e) {
    // 缓存损坏：清除坏缓存并回落默认值（E 系列边界）
    localStorage.removeItem(PREFERENCES_STORAGE_KEY)
    return { ...DEFAULT_PREFERENCES }
  }
}

function writeCache(value) {
  try {
    localStorage.setItem(PREFERENCES_STORAGE_KEY, JSON.stringify(value))
  } catch (e) {
    // 缓存写入失败不阻断主流程，服务端仍是真源
  }
}

function applyThemeToDom(theme) {
  if (typeof document === 'undefined') return
  const value = THEMES.includes(theme) ? theme : 'purple'
  document.documentElement.dataset.theme = value
}

function applyMotionToDom(motion) {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle('no-motion', motion === false)
}

// 模块加载即应用缓存主题/动效，避免首屏闪烁（App 挂载前生效）
const initialPreferences = readCache()
applyThemeToDom(initialPreferences.theme)
applyMotionToDom(initialPreferences.motion)

export const usePreferencesStore = defineStore('preferences', () => {
  const state = ref(initialPreferences)
  const loaded = ref(false)
  const syncing = ref(false)

  function applyTheme() {
    applyThemeToDom(state.value.theme)
  }

  function applyMotion() {
    applyMotionToDom(state.value.motion)
  }

  function commit(next, persistCache = true) {
    state.value = normalize(next)
    applyTheme()
    applyMotion()
    if (persistCache) writeCache(state.value)
  }

  // GET 初始化：成功后用服务端完整对象覆盖本地缓存
  async function load() {
    syncing.value = true
    try {
      const data = await api.get('/user/preferences')
      commit(data)
      loaded.value = true
      return state.value
    } finally {
      syncing.value = false
    }
  }

  // PUT 保存：传部分字段，后端返回完整对象后整体覆盖
  async function save(partial) {
    syncing.value = true
    try {
      const data = await api.put('/user/preferences', partial || {})
      commit(data)
      loaded.value = true
      return state.value
    } finally {
      syncing.value = false
    }
  }

  // 恢复默认：写默认值并 PUT（失败不提示成功，由调用方处理）
  async function reset() {
    return save({ ...DEFAULT_PREFERENCES })
  }

  return {
    state,
    loaded,
    syncing,
    load,
    save,
    reset,
    applyTheme,
    applyMotion
  }
})