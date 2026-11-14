import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import { getExpiresAt, isTokenValid as isJwtTokenValid } from '@/utils/jwt'

// token 过期判定预留 30s 偏差，避免临界时刻仍进入内页
const TOKEN_SKEW_MS = 30000

// 跳转白名单：仅允许站内相对路径（/ 开头且非 //）或与当前页面同源的 http(s) 地址
function isSafeRedirectUrl(url) {
  if (typeof url !== 'string') return false
  const target = url.trim()
  if (!target) return false
  // 协议相对地址（//evil.com）会被浏览器当作跨域，直接拒绝
  if (target.startsWith('//')) return false
  if (target.startsWith('/')) return true
  try {
    const parsed = new URL(target, window.location.origin)
    return (
      (parsed.protocol === 'http:' || parsed.protocol === 'https:') &&
      parsed.origin === window.location.origin
    )
  } catch (e) {
    return false
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const gameStats = ref(null)
  // 从 localStorage.token 解析出的过期时间（毫秒），解析失败为 null
  const tokenExpiresAt = ref(getExpiresAt(token.value))

  // 登录态语义：token 可解析且未过期（不再只是"字符串非空"）
  const isLoggedIn = computed(() => isJwtTokenValid(token.value, TOKEN_SKEW_MS))

  function isTokenValid() {
    return isJwtTokenValid(token.value, TOKEN_SKEW_MS)
  }

  function setToken(newToken) {
    token.value = newToken || ''
    tokenExpiresAt.value = getExpiresAt(token.value)
    if (token.value) {
      localStorage.setItem('token', token.value)
    } else {
      localStorage.removeItem('token')
    }
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  // 第三方登录（保留入口形态，不再跳转非白名单地址）
  async function login(provider) {
    const response = await api.get(`/auth/${provider}`)
    const authUrl = response?.authUrl
    if (!isSafeRedirectUrl(authUrl)) {
      ElMessage.warning('第三方登录暂未开放，请使用手机号登录')
      return
    }
    window.location.href = authUrl
  }

  // 手机号密码登录（主登录方式）
  async function loginWithPhone(phone, password) {
    try {
      const response = await api.post('/auth/login', { phone, password })
      setToken(response.token)
      setUserInfo(response.user)
      return response
    } catch (error) {
      throw error
    }
  }

  async function handleCallback(provider, code) {
    try {
      const response = await api.get(`/auth/callback/${provider}`, { params: { code } })
      setToken(response.token)
      setUserInfo(response.user)
      return response
    } catch (error) {
      throw error
    }
  }

  async function fetchUserInfo() {
    try {
      const response = await api.get('/user/profile')
      setUserInfo(response)
      return response
    } catch (error) {
      throw error
    }
  }

  async function fetchGameStats() {
    try {
      const response = await api.get('/user/stats')
      gameStats.value = response
      return response
    } catch (error) {
      throw error
    }
  }

  async function updateProfile(data) {
    try {
      const response = await api.put('/user/profile', data)
      setUserInfo(response)
      return response
    } catch (error) {
      throw error
    }
  }

  function logout() {
    token.value = ''
    tokenExpiresAt.value = null
    userInfo.value = null
    gameStats.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    // 偏好缓存随登录态一起清理，避免下个账号继承上一个账号的偏好
    localStorage.removeItem('gs_preferences')
  }

  return {
    token,
    tokenExpiresAt,
    userInfo,
    gameStats,
    isLoggedIn,
    isTokenValid,
    setToken,
    setUserInfo,
    login,
    loginWithPhone,
    handleCallback,
    fetchUserInfo,
    fetchGameStats,
    updateProfile,
    logout
  }
})