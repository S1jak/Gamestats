<template>
  <div class="library-page">
    <div class="library-header">
      <div class="header-left">
        <h2>我的游戏库</h2>
        <span class="game-count">{{ filteredGames.length }} 款游戏</span>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchQuery"
          placeholder="搜索游戏..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-select v-model="genreFilter" placeholder="类型" clearable class="filter-select">
          <el-option v-for="genre in genres" :key="genre" :label="genre" :value="genre" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="状态" clearable class="filter-select">
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <div class="view-toggle">
          <button
            :class="['toggle-btn', { active: viewMode === 'grid' }]"
            @click="viewMode = 'grid'"
          >
            <el-icon><Grid /></el-icon>
          </button>
          <button
            :class="['toggle-btn', { active: viewMode === 'list' }]"
            @click="viewMode = 'list'"
          >
            <el-icon><List /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <div v-if="gameStore.loading" class="loading-state">
      <div v-for="i in 12" :key="i" class="skeleton-card">
        <div class="skeleton-cover skeleton"></div>
        <div class="skeleton-title skeleton"></div>
        <div class="skeleton-meta skeleton"></div>
      </div>
    </div>

    <div v-else-if="filteredGames.length === 0" class="empty-state">
      <el-icon :size="64"><FolderOpened /></el-icon>
      <p>没有找到匹配的游戏</p>
      <el-button @click="clearFilters">清除筛选</el-button>
    </div>

    <div v-else :class="['games-container', viewMode]">
      <GameCard
        v-for="game in pagedGames"
        :key="game.id"
        :game="game"
        :view-mode="viewMode"
        @click="$router.push(`/game/${game.id}`)"
        @update-status="handleUpdateStatus"
      />
    </div>

    <div v-if="filteredGames.length > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredGames.length"
        layout="prev, pager, next"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useGameStore } from '@/store/game'
import { ElMessage } from 'element-plus'
import GameCard from '@/components/GameCard.vue'
import { GAME_STATUS_OPTIONS } from '@/utils/gameStatus'

const route = useRoute()
const gameStore = useGameStore()

const searchQuery = ref('')
const genreFilter = ref('')
const statusFilter = ref('')
const viewMode = ref('grid')
const currentPage = ref(1)
const pageSize = 24

// 状态选项：统一引用 gameStatus 常量，与后端中文五值保持一致
const statusOptions = GAME_STATUS_OPTIONS

// 类型选项：从已加载的游戏数据中动态提取唯一 genre，避免与数据库脱节
const genres = computed(() => {
  const set = new Set()
  gameStore.games.forEach(g => {
    if (g.genre) set.add(g.genre)
  })
  return Array.from(set)
})

const filteredGames = computed(() => {
  let result = [...gameStore.games]

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(game =>
      game.name.toLowerCase().includes(query)
    )
  }

  if (genreFilter.value) {
    result = result.filter(game => game.genre === genreFilter.value)
  }

  if (statusFilter.value) {
    result = result.filter(game => game.status === statusFilter.value)
  }

  return result
})

// 当前页切片：网格/列表视图共用同一页码，pageSize 只在此处定义
const pagedGames = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredGames.value.slice(start, start + pageSize)
})

// 筛选条件任一变化时回到第 1 页
watch([searchQuery, genreFilter, statusFilter], () => {
  currentPage.value = 1
})

// 过滤结果减少导致当前页越界时，钳制到最后一页（结果为空则停在 1）
watch(filteredGames, (list) => {
  const maxPage = Math.max(1, Math.ceil(list.length / pageSize))
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
  }
})

function clearFilters() {
  searchQuery.value = ''
  genreFilter.value = ''
  statusFilter.value = ''
}

async function handleUpdateStatus(gameId, status) {
  try {
    await gameStore.updateGameStatus(gameId, status)
    ElMessage.success('状态已更新')
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  }
}

watch(() => route.query.search, (val) => {
  if (val) {
    searchQuery.value = val
  }
}, { immediate: true })

onMounted(() => {
  gameStore.fetchGames()
})
</script>

<style lang="scss" scoped>
.library-page {
  max-width: 1400px;
  margin: 0 auto;
}

.library-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;

  .header-left {
    display: flex;
    align-items: baseline;
    gap: 12px;

    h2 {
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
    }

    .game-count {
      font-size: 14px;
      color: $text-secondary;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.search-input {
  width: 200px;
}

.filter-select {
  width: 120px;
}

.view-toggle {
  display: flex;
  background: $bg-card;
  border-radius: $radius-sm;
  padding: 4px;

  .toggle-btn {
    padding: 8px 12px;
    border: none;
    background: transparent;
    color: $text-secondary;
    cursor: pointer;
    border-radius: 4px;
    transition: all $transition-fast;

    &:hover {
      color: $text-primary;
    }

    &.active {
      background: $primary-color;
      color: white;
    }
  }
}

.loading-state {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.skeleton-card {
  background: $bg-card;
  border-radius: $radius-lg;
  overflow: hidden;

  .skeleton-cover {
    height: 280px;
  }

  .skeleton-title {
    height: 20px;
    margin: 12px;
    width: 70%;
  }

  .skeleton-meta {
    height: 14px;
    margin: 0 12px 12px;
    width: 50%;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: $text-secondary;

  .el-icon {
    margin-bottom: 16px;
    color: $text-muted;
  }

  p {
    margin-bottom: 20px;
  }
}

.games-container {
  &.grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
  }

  &.list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
