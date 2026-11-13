import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/utils/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const gameStats = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  async function login(provider) {
    try {
      const response = await api.get(`/auth/${provider}`)
      window.location.href = response.authUrl
    } catch (error) {
      throw error
    }
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
    userInfo.value = null
    gameStats.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    gameStats,
    isLoggedIn,
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
