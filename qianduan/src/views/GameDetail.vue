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
            <span class="genre">{{ game.genre }}</span>
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
          <div ref="playTimeChartRef" class="chart-container"></div>
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
            <el-option label="在玩" value="在玩" />
            <el-option label="已完成" value="已完成" />
            <el-option label="未玩" value="未玩" />
            <el-option label="搁置" value="搁置" />
            <el-option label="弃坑" value="弃坑" />
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
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useGameStore } from '@/store/game'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const route = useRoute()
const gameStore = useGameStore()
const playTimeChartRef = ref(null)
const newTag = ref('')

// 游戏详情：初始空结构避免模板渲染报错，onMounted 后由 API 填充
const game = ref({
  tags: [],
  platforms: [],
  achievementList: []
})

// 成就列表来自后端 GameDetailVO.achievementList
const achievements = computed(() => game.value.achievementList || [])

// 评分按 10 分制存储，el-rate 满星为 5，除以 2 用于星星显示
const ratingStars = computed(() => (Number(game.value.rating) || 0) / 2)

// 按路由参数 id 加载对应游戏详情
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

function formatPlayTime(minutes) {
  if (!minutes) return '0小时'
  const hours = Math.floor(minutes / 60)
  return `${hours}小时`
}

function initPlayTimeChart() {
  if (!playTimeChartRef.value) return

  const chart = echarts.init(playTimeChartRef.value)
  const days = []
  const data = []

  for (let i = 29; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    days.push(`${date.getMonth() + 1}/${date.getDate()}`)
    data.push(Math.floor(Math.random() * 120 + 30))
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
      data: days,
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
      data,
      type: 'bar',
      barWidth: 12,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: '#6366f1'
      }
    }]
  }

  chart.setOption(option)
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

function addTag() {
  const tag = newTag.value.trim()
  const tags = game.value.tags || []
  if (tag && !tags.includes(tag)) {
    tags.push(tag)
    newTag.value = ''
  }
}

function removeTag(tag) {
  const tags = game.value.tags || []
  const index = tags.indexOf(tag)
  if (index > -1) {
    tags.splice(index, 1)
  }
}

onMounted(async () => {
  await loadGame()
  initPlayTimeChart()
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
