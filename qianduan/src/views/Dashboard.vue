<template>
  <div class="dashboard-page">
    <div class="stats-cards">
      <StatCard
        title="总游戏时长"
        :value="formatPlayTime(stats.totalPlayTime)"
        icon="Timer"
        color="#6366f1"
        :trend="stats.playTimeTrend"
      />
      <StatCard
        title="游戏数量"
        :value="stats.totalGames"
        icon="Grid"
        color="#22d3ee"
        :trend="stats.gamesTrend"
      />
      <StatCard
        title="总游玩天数"
        :value="stats.playDays"
        icon="Calendar"
        color="#f472b6"
      />
      <StatCard
        title="成就解锁"
        :value="`${stats.achievements}/${stats.totalAchievements}`"
        icon="Trophy"
        color="#fbbf24"
        :progress="achievementProgress"
      />
    </div>
    
    <div class="charts-row">
      <div class="chart-card large">
        <div class="card-header">
          <h3>游玩时长趋势</h3>
          <div class="time-tabs">
            <button 
              v-for="tab in timeTabs" 
              :key="tab.value"
              :class="['tab', { active: activeTimeTab === tab.value }]"
              @click="activeTimeTab = tab.value"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      
      <div class="chart-card">
        <div class="card-header">
          <h3>游戏类型分布</h3>
        </div>
        <div ref="genreChartRef" class="chart-container"></div>
      </div>
    </div>
    
    <div class="charts-row">
      <div class="chart-card">
        <div class="card-header">
          <h3>游玩热力图</h3>
          <el-select v-model="heatMapYear" size="small" style="width: 100px">
            <el-option v-for="year in years" :key="year" :label="year" :value="year" />
          </el-select>
        </div>
        <div ref="heatMapRef" class="heatmap-container"></div>
      </div>
      
      <div class="chart-card">
        <div class="card-header">
          <h3>近期游戏排行</h3>
          <el-select v-model="rankPeriod" size="small" style="width: 100px">
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
            <el-option label="全部" value="all" />
          </el-select>
        </div>
        <div class="game-rank-list">
          <div 
            v-for="(game, index) in recentGames" 
            :key="game.id" 
            class="rank-item"
            @click="$router.push(`/game/${game.id}`)"
          >
            <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <img :src="game.cover" :alt="game.name" class="game-cover" />
            <div class="game-info">
              <span class="game-name">{{ game.name }}</span>
              <span class="game-time">{{ formatPlayTime(game.playTime) }}</span>
            </div>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>
    
    <div class="achievements-section">
      <div class="card-header">
        <h3>成就进度</h3>
        <router-link to="/library" class="view-all">查看全部</router-link>
      </div>
      <div class="achievement-cards">
        <div 
          v-for="game in achievementGames" 
          :key="game.id" 
          class="achievement-card"
        >
          <img :src="game.cover" :alt="game.name" class="game-cover" />
          <div class="achievement-info">
            <span class="game-name">{{ game.name }}</span>
            <el-progress 
              :percentage="game.achievementProgress" 
              :stroke-width="6"
              :color="getProgressColor(game.achievementProgress)"
            />
            <span class="achievement-count">
              {{ game.unlockedAchievements }} / {{ game.totalAchievements }} 成就
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import StatCard from '@/components/StatCard.vue'
import { useGameStore } from '@/store/game'
import api from '@/utils/api'

const gameStore = useGameStore()

const trendChartRef = ref(null)
const genreChartRef = ref(null)
const heatMapRef = ref(null)

let trendChart = null
let genreChart = null
let heatMapChart = null

// 类型分布饼图配色
const GENRE_COLORS = ['#6366f1', '#22d3ee', '#f472b6', '#fbbf24', '#22c55e', '#71717a', '#a78bfa', '#f87171']

const timeTabs = [
  { label: '30天', value: 30 },
  { label: '90天', value: 90 },
  { label: '365天', value: 365 }
]

const activeTimeTab = ref(30)
const heatMapYear = ref(dayjs().year())
const rankPeriod = ref('week')

const years = computed(() => {
  const currentYear = dayjs().year()
  return Array.from({ length: 5 }, (_, i) => currentYear - i)
})

// 统计卡片：初始置零，onMounted 时从 /dashboard/stats 拉取真实数据
const stats = ref({
  totalPlayTime: 0,
  totalGames: 0,
  playDays: 0,
  achievements: 0,
  totalAchievements: 0,
  completedGames: 0,
  playTimeTrend: 0,
  gamesTrend: 0
})

const achievementProgress = computed(() => {
  const total = stats.value.totalAchievements || 0
  if (!total) return 0
  return Math.round((stats.value.achievements / total) * 100)
})

// 近期游戏排行：onMounted 及 rankPeriod 变化时从后端拉取
const recentGames = ref([])

// 成就进度榜：取数据库中该用户游戏按成就完成率排序的前 3 款
const achievementGames = computed(() => {
  return gameStore.games
    .filter(g => g.totalAchievements && g.totalAchievements > 0)
    .map(g => ({
      id: g.id,
      name: g.name,
      cover: g.cover,
      unlockedAchievements: g.achievements || 0,
      totalAchievements: g.totalAchievements,
      achievementProgress: Math.round(((g.achievements || 0) / g.totalAchievements) * 100)
    }))
    .sort((a, b) =>
      b.achievementProgress - a.achievementProgress ||
      b.unlockedAchievements - a.unlockedAchievements
    )
    .slice(0, 3)
})

function formatPlayTime(minutes) {
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时`
  const days = Math.floor(hours / 24)
  const remainHours = hours % 24
  return `${days}天${remainHours}小时`
}

function getProgressColor(progress) {
  if (progress >= 100) return '#22c55e'
  if (progress >= 50) return '#6366f1'
  return '#f59e0b'
}

// ---- 数据加载 ----
async function loadStats() {
  try {
    stats.value = await api.get('/dashboard/stats')
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  }
}

async function loadRecentGames() {
  try {
    recentGames.value = await api.get('/dashboard/recent-games', {
      params: { period: rankPeriod.value, limit: 5 }
    })
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  }
}

// ---- 图表渲染 ----
function renderTrendChart(records) {
  if (!trendChart) return
  // 后端仅返回有记录的日期，前端按时间范围补全缺失日期为 0
  const map = new Map()
  ;(records || []).forEach(r => map.set(r.date, r.minutes))
  const days = activeTimeTab.value
  const dates = []
  const data = []
  for (let i = days - 1; i >= 0; i--) {
    const d = dayjs().subtract(i, 'day')
    dates.push(d.format('MM/DD'))
    data.push(map.get(d.format('YYYY-MM-DD')) || 0)
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 26, 46, 0.9)',
      borderColor: '#27273f',
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#27273f' } },
      axisLabel: { color: '#71717a', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#71717a', fontSize: 11 },
      splitLine: { lineStyle: { color: '#27273f' } }
    },
    series: [{
      data,
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: {
        width: 3,
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#6366f1' },
          { offset: 1, color: '#22d3ee' }
        ])
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(99, 102, 241, 0.3)' },
          { offset: 1, color: 'rgba(99, 102, 241, 0)' }
        ])
      }
    }]
  }
  trendChart.setOption(option, true)
}

function renderGenreChart(list) {
  if (!genreChart) return
  const data = (list || []).map((g, i) => ({
    value: g.percent,
    name: g.name,
    itemStyle: { color: GENRE_COLORS[i % GENRE_COLORS.length] }
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(26, 26, 46, 0.9)',
      borderColor: '#27273f',
      textStyle: { color: '#fff' },
      formatter: '{b}: {d}%'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#a1a1aa', fontSize: 12 }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#1a1a2e',
        borderWidth: 2
      },
      label: { show: false },
      data
    }]
  }
  genreChart.setOption(option, true)
}

function renderHeatmap(records) {
  if (!heatMapChart) return
  const year = heatMapYear.value
  const data = (records || []).map(r => [r.date, r.minutes])
  const maxVal = data.length ? Math.max(...data.map(d => d[1])) : 0

  const option = {
    tooltip: {
      position: 'top',
      backgroundColor: 'rgba(26, 26, 46, 0.9)',
      borderColor: '#27273f',
      textStyle: { color: '#fff' },
      formatter: (params) => `${params.value[0]}: ${params.value[1]}分钟`
    },
    visualMap: {
      min: 0,
      max: Math.max(maxVal, 60),
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      show: false,
      inRange: {
        color: ['#1a1a2e', '#312e81', '#6366f1', '#818cf8']
      }
    },
    calendar: {
      top: 40,
      left: 30,
      right: 30,
      cellSize: ['auto', 14],
      range: year,
      itemStyle: {
        borderWidth: 2,
        borderColor: '#0f0f23'
      },
      yearLabel: { show: false },
      monthLabel: {
        nameMap: 'ZH',
        color: '#71717a',
        fontSize: 11
      },
      dayLabel: {
        nameMap: 'ZH',
        color: '#71717a',
        fontSize: 10
      },
      splitLine: {
        lineStyle: { color: '#27273f', width: 2 }
      }
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data
    }]
  }
  heatMapChart.setOption(option, true)
}

// ---- 数据加载 + 渲染 ----
async function loadPlayTrend() {
  try {
    const records = await api.get('/dashboard/play-trend', { params: { days: activeTimeTab.value } })
    renderTrendChart(records)
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  }
}

async function loadGenreDistribution() {
  try {
    const list = await api.get('/dashboard/genre-distribution')
    renderGenreChart(list)
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  }
}

async function loadHeatmap() {
  try {
    const records = await api.get('/dashboard/heatmap', { params: { year: heatMapYear.value } })
    renderHeatmap(records)
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  }
}

function handleResize() {
  trendChart?.resize()
  genreChart?.resize()
  heatMapChart?.resize()
}

watch(activeTimeTab, () => loadPlayTrend())
watch(heatMapYear, () => loadHeatmap())
watch(rankPeriod, () => loadRecentGames())

onMounted(async () => {
  await nextTick()
  if (trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (genreChartRef.value) genreChart = echarts.init(genreChartRef.value)
  if (heatMapRef.value) heatMapChart = echarts.init(heatMapRef.value)

  // 拉取真实数据
  loadStats()
  loadRecentGames()
  loadPlayTrend()
  loadGenreDistribution()
  loadHeatmap()

  // 加载游戏数据用于成就进度榜展示（已加载则跳过）
  if (!gameStore.games.length) {
    gameStore.fetchGames().catch(() => {
      // 错误已由 axios 拦截器统一提示
    })
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  genreChart?.dispose()
  heatMapChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
  
  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
  
  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  
  &.large {
    grid-column: span 1;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    h3 {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
    }
  }
}

.time-tabs {
  display: flex;
  gap: 4px;
  background: $bg-darker;
  padding: 4px;
  border-radius: $radius-sm;
  
  .tab {
    padding: 6px 12px;
    border: none;
    background: transparent;
    color: $text-secondary;
    font-size: 12px;
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

.chart-container {
  height: 280px;
}

.heatmap-container {
  height: 180px;
}

.game-rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: $bg-darker;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  
  &:hover {
    background: $bg-card-hover;
    transform: translateX(4px);
  }
  
  .rank {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: $text-secondary;
    background: $bg-card;
    border-radius: 4px;
    
    &.top {
      background: linear-gradient(135deg, $primary-color, $secondary-color);
      color: white;
    }
  }
  
  .game-cover {
    width: 40px;
    height: 54px;
    object-fit: cover;
    border-radius: 4px;
  }
  
  .game-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .game-name {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
    }
    
    .game-time {
      font-size: 12px;
      color: $text-secondary;
    }
  }
  
  .arrow {
    color: $text-muted;
  }
}

.achievements-section {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    h3 {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
    }
    
    .view-all {
      font-size: 13px;
      color: $primary-color;
    }
  }
}

.achievement-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  
  @media (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.achievement-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: $bg-darker;
  border-radius: $radius-md;
  transition: all $transition-fast;
  
  &:hover {
    background: $bg-card-hover;
  }
  
  .game-cover {
    width: 60px;
    height: 80px;
    object-fit: cover;
    border-radius: 6px;
  }
  
  .achievement-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
    
    .game-name {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
    }
    
    .achievement-count {
      font-size: 12px;
      color: $text-secondary;
    }
  }
}
</style>
