<template>
  <div class="game-detail-page">
    <div class="game-header">
      <button class="back-btn" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </button>
    </div>
    
    <div class="game-hero">
      <img :src="game.cover" :alt="game.name" class="hero-cover" />
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <img :src="game.cover" :alt="game.name" class="game-poster" />
        <div class="game-info">
          <h1>{{ game.name }}</h1>
          <div class="game-meta">
            <span v-for="genreItem in displayGenres" :key="genreItem" class="genre">{{ genreItem }}</span>
            <span class="developer">{{ game.developer }}</span>
            <span class="release-date">{{ game.releaseDate }}</span>
          </div>
          <div class="game-rating">
            <el-rate :model-value="ratingStars" disabled text-color="#ff9900" />
            <span class="rating-text">{{ game.rating }} 分</span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="game-content">
      <div class="main-column">
        <div class="stats-card">
          <div class="stat-item">
            <el-icon :size="24"><Timer /></el-icon>
            <div class="stat-info">
              <span class="value">{{ formatPlayTime(game.playTime) }}</span>
              <span class="label">游玩时长</span>
            </div>
          </div>
          <div class="stat-item">
            <el-icon :size="24"><Calendar /></el-icon>
            <div class="stat-info">
              <span class="value">{{ game.sessions }}次</span>
              <span class="label">游玩次数</span>
            </div>
          </div>
          <div class="stat-item">
            <el-icon :size="24"><Clock /></el-icon>
            <div class="stat-info">
              <span class="value">{{ game.lastPlayed }}</span>
              <span class="label">最后游玩</span>
            </div>
          </div>
          <div class="stat-item">
            <el-icon :size="24"><Trophy /></el-icon>
            <div class="stat-info">
              <span class="value">{{ game.achievements }}/{{ game.totalAchievements }}</span>
              <span class="label">成就进度</span>
            </div>
          </div>
        </div>
        
        <div class="section">
          <h3>游玩时长趋势</h3>
          <div v-if="trendLoading" class="chart-empty">加载中...</div>
          <div v-else-if="!hasTrendData" class="chart-empty">近 30 天暂无游玩记录</div>
          <div v-else ref="playTimeChartRef" class="chart-container"></div>
        </div>
        
        <div class="section">
          <h3>我的笔记</h3>
          <div class="notes-editor">
            <el-input
              v-model="game.note"
              type="textarea"
              :rows="4"
              placeholder="记录你的游戏心得..."
              @blur="saveNote"
            />
          </div>
        </div>
        
        <div class="section">
          <h3>成就列表</h3>
          <div class="achievements-grid">
            <div 
              v-for="achievement in achievements" 
              :key="achievement.id" 
              class="achievement-item"
              :class="{ unlocked: achievement.unlocked }"
            >
              <div class="achievement-icon">
                <img :src="achievement.icon" :alt="achievement.name" />
              </div>
              <div class="achievement-info">
                <span class="name">{{ achievement.name }}</span>
                <span class="desc">{{ achievement.description }}</span>
                <span v-if="achievement.unlockedDate" class="date">
                  解锁于 {{ achievement.unlockedDate }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="side-column">
        <div class="status-card">
          <h4>游戏状态</h4>
          <el-select v-model="game.status" @change="updateStatus">
            <el-option
              v-for="opt in GAME_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        
        <div class="tags-card">
          <h4>自定义标签</h4>
          <div class="tags-list">
            <el-tag 
              v-for="tag in game.tags" 
              :key="tag" 
              closable
              @close="removeTag(tag)"
            >
              {{ tag }}
            </el-tag>
          </div>
          <el-input
            v-model="newTag"
            placeholder="添加标签"
            size="small"
            @keyup.enter="addTag"
          >
            <template #append>
              <el-button @click="addTag">添加</el-button>
            </template>
          </el-input>
        </div>
        
        <div class="platform-card">
          <h4>游戏平台</h4>
          <div class="platforms">
            <div 
              v-for="platform in game.platforms" 
              :key="platform.name" 
              class="platform-item"
              :class="{ active: platform.owned }"
            >
              <span class="name">{{ platform.name }}</span>
              <el-icon v-if="platform.owned"><Check /></el-icon>
            </div>
          </div>
        </div>
        
        <div class="share-card">
          <h4>分享</h4>
          <div class="share-buttons">
            <el-button>
              <el-icon><Share /></el-icon>
              分享到 Discord
            </el-button>
            <el-button>
              <el-icon><Download /></el-icon>
              导出数据
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useGameStore } from '@/store/game'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { GAME_STATUS_OPTIONS } from '@/utils/gameStatus'

const route = useRoute()
const gameStore = useGameStore()
const playTimeChartRef = ref(null)
const newTag = ref('')

// 游戏详情：初始空结构避免模板渲染报错，加载后由 API 填充
const game = ref({
  tags: [],
  genres: [],
  platforms: [],
  achievementList: []
})

// 标签本地校验规则，与后端保持一致（trim、单标签 1-20 字、最多 10 个）
const MAX_TAG_COUNT = 10
const MAX_TAG_LENGTH = 20

// 成就列表来自后端 GameDetailVO.achievementList
const achievements = computed(() => game.value.achievementList || [])

// 评分按 10 分制存储，el-rate 满星为 5，除以 2 用于星星显示
const ratingStars = computed(() => (Number(game.value.rating) || 0) / 2)

// 类型区域只渲染 genres（全部类型），旧数据缺失时退回主类型 genre
const displayGenres = computed(() => {
  const list = game.value.genres
  if (Array.isArray(list) && list.length) return list
  return game.value.genre ? [game.value.genre] : []
})

// 趋势区间：今天往前 29 天至今天，与后端 days=30 窗口一致
const TREND_DAYS = 30
const trendLabels = ref([])
const trendMinutes = ref([])
const trendLoading = ref(true)
const hasTrendData = computed(() => trendMinutes.value.some(v => v > 0))

let playTimeChart = null

// 按路由参数 id 加载游戏详情与游玩趋势
async function loadGame() {
  const id = route.params.id
  if (!id) return
  try {
    await gameStore.fetchGameById(id)
    if (gameStore.currentGame) {
      game.value = gameStore.currentGame
    }
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  }
}

// 拉取真实按日趋势并补零，禁止任何客户端随机数据
async function loadPlayTrend() {
  const id = route.params.id
  if (!id) return
  trendLoading.value = true
  try {
    const records = await gameStore.fetchPlayTrend(id, TREND_DAYS)
    const map = new Map()
    ;(records || []).forEach(r => map.set(r.date, Number(r.minutes) || 0))

    const labels = []
    const minutes = []
    for (let i = TREND_DAYS - 1; i >= 0; i--) {
      const d = dayjs().subtract(i, 'day')
      labels.push(d.format('M/D'))
      minutes.push(map.get(d.format('YYYY-MM-DD')) || 0)
    }
    trendLabels.value = labels
    trendMinutes.value = minutes
  } catch (e) {
    trendLabels.value = []
    trendMinutes.value = []
  } finally {
    trendLoading.value = false
  }
}

function formatPlayTime(minutes) {
  if (!minutes) return '0小时'
  const hours = Math.floor(minutes / 60)
  return `${hours}小时`
}

function renderPlayTimeChart() {
  if (!playTimeChartRef.value || !hasTrendData.value) {
    disposePlayTimeChart()
    return
  }
  if (playTimeChart) playTimeChart.dispose()
  playTimeChart = echarts.init(playTimeChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 26, 46, 0.9)',
      borderColor: '#27273f',
      textStyle: { color: '#fff' },
      formatter: '{b}: {c} 分钟'
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
      data: trendLabels.value,
      axisLine: { lineStyle: { color: '#27273f' } },
      axisLabel: { color: '#71717a', fontSize: 10 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#71717a' },
      splitLine: { lineStyle: { color: '#27273f' } }
    },
    series: [{
      data: trendMinutes.value,
      type: 'bar',
      barWidth: 12,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: '#6366f1'
      }
    }]
  }

  playTimeChart.setOption(option)
}

function disposePlayTimeChart() {
  if (playTimeChart) {
    playTimeChart.dispose()
    playTimeChart = null
  }
}

async function saveNote() {
  try {
    await gameStore.updateGameNote(game.value.id, game.value.note)
    ElMessage.success('笔记已保存')
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function updateStatus() {
  try {
    await gameStore.updateGameStatus(game.value.id, game.value.status)
    ElMessage.success('状态已更新')
  } catch (e) {
    // 错误已由拦截器提示
  }
}

// 标签持久化：先乐观更新本地，再调用接口；失败回滚到请求前集合
async function persistTags(nextTags) {
  const previous = [...(game.value.tags || [])]
  game.value = { ...game.value, tags: nextTags }
  try {
    const response = await gameStore.updateGameTags(game.value.id, nextTags)
    if (response && Array.isArray(response.tags)) {
      game.value = { ...game.value, tags: response.tags }
    }
  } catch (e) {
    game.value = { ...game.value, tags: previous }
    ElMessage.error('标签保存失败，已恢复原标签')
  }
}

async function addTag() {
  const tag = newTag.value.trim()
  if (!tag) return

  const tags = game.value.tags || []
  if (tags.includes(tag)) {
    newTag.value = ''
    return
  }
  if ([...tag].length > MAX_TAG_LENGTH) {
    ElMessage.warning(`单个标签长度需为1-${MAX_TAG_LENGTH}个字符`)
    return
  }
  if (tags.length >= MAX_TAG_COUNT) {
    ElMessage.warning(`标签不能超过${MAX_TAG_COUNT}个`)
    return
  }

  newTag.value = ''
  await persistTags([...tags, tag])
}

async function removeTag(tag) {
  const tags = game.value.tags || []
  if (!tags.includes(tag)) return
  await persistTags(tags.filter(t => t !== tag))
}

// 切换游戏（同一路由组件复用）时重新拉取详情与趋势，避免残留上一款数据
watch(() => route.params.id, (id, oldId) => {
  if (!id || id === oldId) return
  newTag.value = ''
  loadGame().then(loadPlayTrend)
})

onMounted(async () => {
  await loadGame()
  await loadPlayTrend()
})

watch([hasTrendData, trendLoading], async () => {
  await nextTick()
  renderPlayTimeChart()
})

onUnmounted(() => {
  disposePlayTimeChart()
})
</script>

<style lang="scss" scoped>
.game-detail-page {
  max-width: 1200px;
  margin: 0 auto;
}

.game-header {
  margin-bottom: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: transparent;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;
  
  &:hover {
    background: $bg-card;
    color: $text-primary;
  }
}

.game-hero {
  position: relative;
  height: 300px;
  border-radius: $radius-lg;
  overflow: hidden;
  margin-bottom: 32px;
  
  .hero-cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    filter: blur(20px);
    transform: scale(1.1);
  }
  
  .hero-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(to right, rgba($bg-dark, 0.9) 0%, rgba($bg-dark, 0.5) 100%);
  }
  
  .hero-content {
    position: absolute;
    inset: 0;
    display: flex;
    gap: 32px;
    padding: 32px;
    align-items: flex-end;
  }
  
  .game-poster {
    width: 180px;
    height: 240px;
    object-fit: cover;
    border-radius: $radius-md;
    box-shadow: $shadow-lg;
  }
  
  .game-info {
    flex: 1;
    
    h1 {
      font-size: 32px;
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 12px;
    }
    
    .game-meta {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;

      span {
        font-size: 14px;
        color: $text-secondary;

        &.genre {
          color: $primary-color;
        }
      }
    }

    .game-rating {
      display: flex;
      align-items: center;
      gap: 12px;

      .rating-text {
        font-size: 18px;
        font-weight: 600;
        color: #ff9900;
      }
    }
  }
}

.game-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  
  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stats-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stat-item {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .el-icon {
      color: $primary-color;
    }
    
    .stat-info {
      display: flex;
      flex-direction: column;
      
      .value {
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
}

.section {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 24px;
  
  h3 {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 16px;
  }
}

.chart-container {
  height: 200px;
}

.chart-empty {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: $text-muted;
}

.notes-editor {
  :deep(.el-textarea__inner) {
    background: $bg-darker;
    border-color: $border-color;
    color: $text-primary;
    
    &:focus {
      border-color: $primary-color;
    }
  }
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.achievement-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: $bg-darker;
  border-radius: $radius-md;
  opacity: 0.5;
  
  &.unlocked {
    opacity: 1;
    
    .achievement-icon {
      border-color: $primary-color;
    }
  }
  
  .achievement-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    border: 2px solid $border-color;
    overflow: hidden;
    flex-shrink: 0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .achievement-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .name {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
    }
    
    .desc {
      font-size: 12px;
      color: $text-secondary;
    }
    
    .date {
      font-size: 11px;
      color: $primary-color;
    }
  }
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-card,
.tags-card,
.platform-card,
.share-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  
  h4 {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
  }
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  
  .el-tag {
    background: rgba($primary-color, 0.1);
    border-color: rgba($primary-color, 0.3);
    color: $primary-color;
  }
}

.platforms {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.platform-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: $bg-darker;
  border-radius: $radius-sm;
  opacity: 0.5;
  
  &.active {
    opacity: 1;
    
    .name {
      color: $text-primary;
    }
    
    .el-icon {
      color: $success-color;
    }
  }
  
  .name {
    font-size: 13px;
    color: $text-secondary;
  }
}

.share-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
