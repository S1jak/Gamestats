import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/utils/api'

export const useGameStore = defineStore('game', () => {
  const games = ref([])
  const currentGame = ref(null)
  const loading = ref(false)
  const filters = ref({
    search: '',
    genre: '',
    status: '',
    sortBy: 'playTime',
    sortOrder: 'desc'
  })

  const filteredGames = computed(() => {
    let result = [...games.value]
    
    if (filters.value.search) {
      const search = filters.value.search.toLowerCase()
      result = result.filter(game => 
        game.name.toLowerCase().includes(search)
      )
    }
    
    if (filters.value.genre) {
      result = result.filter(game => game.genre === filters.value.genre)
    }
    
    if (filters.value.status) {
      result = result.filter(game => game.status === filters.value.status)
    }
    
    result.sort((a, b) => {
      const { sortBy, sortOrder } = filters.value
      let aVal = a[sortBy]
      let bVal = b[sortBy]
      
      if (typeof aVal === 'string') {
        aVal = aVal.toLowerCase()
        bVal = bVal.toLowerCase()
      }
      
      if (sortOrder === 'asc') {
        return aVal > bVal ? 1 : -1
      }
      return aVal < bVal ? 1 : -1
    })
    
    return result
  })

  const totalPlayTime = computed(() => {
    return games.value.reduce((sum, game) => sum + (game.playTime || 0), 0)
  })

  const totalGames = computed(() => games.value.length)

  async function fetchGames() {
    loading.value = true
    try {
      const response = await api.get('/games')
      games.value = response
      return response
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchGameById(id) {
    loading.value = true
    try {
      const response = await api.get(`/games/${id}`)
      currentGame.value = response
      return response
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  async function updateGameNote(gameId, note) {
    try {
      const response = await api.put(`/games/${gameId}/note`, { note })
      const index = games.value.findIndex(g => g.id === gameId)
      if (index !== -1) {
        games.value[index] = { ...games.value[index], note }
      }
      return response
    } catch (error) {
      throw error
    }
  }

  async function updateGameStatus(gameId, status) {
    try {
      const response = await api.put(`/games/${gameId}/status`, { status })
      const index = games.value.findIndex(g => g.id === gameId)
      if (index !== -1) {
        games.value[index] = { ...games.value[index], status }
      }
      return response
    } catch (error) {
      throw error
    }
  }

  // 按日游玩趋势：GET /games/{gameId}/play-trend?days=N
  // 后端仅返回有记录的日期（{date, minutes}），补齐零值由调用方负责
  async function fetchPlayTrend(gameId, days = 30) {
    const response = await api.get(`/games/${gameId}/play-trend`, { params: { days } })
    return response || []
  }

  // 标签全量替换：PUT /games/{gameId}/tags，body {tags:[...]}
  // 成功后用响应覆盖列表项与当前详情，保证本地与库一致
  async function updateGameTags(gameId, tags) {
    const response = await api.put(`/games/${gameId}/tags`, { tags })
    const index = games.value.findIndex(g => g.id === gameId)
    if (index !== -1) {
      games.value[index] = { ...games.value[index], ...response }
    }
    if (currentGame.value && currentGame.value.id === gameId) {
      currentGame.value = { ...currentGame.value, ...response }
    }
    return response
  }

  function setFilters(newFilters) {
    filters.value = { ...filters.value, ...newFilters }
  }

  function clearFilters() {
    filters.value = {
      search: '',
      genre: '',
      status: '',
      sortBy: 'playTime',
      sortOrder: 'desc'
    }
  }

  return {
    games,
    currentGame,
    loading,
    filters,
    filteredGames,
    totalPlayTime,
    totalGames,
    fetchGames,
    fetchGameById,
    fetchPlayTrend,
    updateGameNote,
    updateGameStatus,
    updateGameTags,
    setFilters,
    clearFilters
  }
})
