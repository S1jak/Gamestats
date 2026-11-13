<template>
  <div class="year-report-page">
    <div class="report-header">
      <h1>年度游戏总结</h1>
      <div class="report-header-actions">
        <el-select v-model="selectedYear" size="large" style="width: 120px">
          <el-option v-for="year in years" :key="year" :label="year + '年'" :value="year" />
        </el-select>
        <el-button
          type="primary"
          size="large"
          :loading="generating"
          :disabled="generating"
          @click="generateSnapshot"
        >
          {{ hasSnapshot ? '刷新快照' : '生成快照' }}
        </el-button>
        <span v-if="hasSnapshot" class="snapshot-tip">快照数据 · 生成于 {{ reportData.snapshotGeneratedAt }}</span>
        <span v-else class="snapshot-tip realtime">实时计算</span>
      </div>
    </div>
    
    <div class="report-poster" ref="posterRef">
      <div class="poster-bg">
        <div class="poster-gradient"></div>
      </div>
      
      <div class="poster-content">
        <div class="poster-title">
          <span class="year">{{ selectedYear }}</span>
          <span class="subtitle">年度游戏总结</span>
        </div>
        
        <div class="poster-avatar">
          <el-avatar :size="80" :src="userInfo.avatar">
            {{ userInfo.username?.charAt(0) }}
          </el-avatar>
          <span class="username">{{ userInfo.username }}</span>
        </div>
        
        <div class="poster-stats">
          <div class="stat-item main">
            <span class="value">{{ reportData.totalHours }}</span>
            <span class="label">小时游戏时长</span>
          </div>
          <div class="stat-row">
            <div class="stat-item">
              <span class="value">{{ reportData.totalGames }}</span>
              <span class="label">款游戏</span>
            </div>
            <div class="stat-item">
              <span class="value">{{ reportData.achievements }}</span>
              <span class="label">个成就</span>
            </div>
            <div class="stat-item">
              <span class="value">{{ reportData.completedGames }}</span>
              <span class="label">款通关</span>
            </div>
          </div>
        </div>
        
        <div class="poster-favorite">
          <h3>年度最爱</h3>
          <div v-if="favoriteGame" class="favorite-game">
            <div class="favorite-cover">
              <img :src="favoriteGame.cover" :alt="favoriteGame.name" />
            </div>
            <div class="game-info">
              <span class="game-name">{{ favoriteGame.name }}</span>
              <span class="game-hours">{{ favoriteGame.hours }}小时</span>
            </div>
          </div>
          <div v-else class="favorite-empty">暂无最爱游戏</div>
        </div>
        
        <div class="poster-tags">
          <span class="tag" v-for="tag in reportTags" :key="tag">{{ tag }}</span>
        </div>
        
        <div class="poster-genres">
          <h3>游戏类型分布</h3>
          <div class="genre-bars">
            <div v-for="genre in reportGenres" :key="genre.name" class="genre-bar">
              <span class="genre-name">{{ genre.name }}</span>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: genre.percent + '%', background: genre.color }"></div>
              </div>
              <span class="genre-percent">{{ genre.percent }}%</span>
            </div>
          </div>
        </div>
        
        <div class="poster-footer">
          <span class="brand">GameStats</span>
          <span class="slogan">记录你的游戏人生</span>
        </div>
      </div>
    </div>
    
    <div class="report-actions">
      <el-button type="primary" size="large" @click="downloadPoster">
        <el-icon><Download /></el-icon>
        保存海报
      </el-button>
      <el-button size="large" @click="shareToDiscord">
        <el-icon><Share /></el-icon>
        分享到 Discord
      </el-button>
    </div>
    
    <div class="report-details">
      <div class="detail-section">
        <h3>游戏时长趋势</h3>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      
      <div class="detail-section">
        <h3>月度游戏排行</h3>
        <div class="monthly-ranking">
          <div v-for="(month, index) in monthlyData" :key="month.month" class="month-item">
            <span class="month-name">{{ month.month }}月</span>
            <div class="month-games" v-if="month.topGames && month.topGames.length">
              <img v-for="game in month.topGames" :key="game.id" :src="game.cover" :alt="game.name" :title="game.name" />
            </div>
            <div class="month-games" v-else></div>
            <span class="month-hours">{{ month.hours }}h</span>
          </div>
        </div>
      </div>
      
      <div class="detail-section">
        <h3>成就里程碑</h3>
        <div class="milestones">
          <div v-for="milestone in milestones" :key="milestone.id" class="milestone-item" :class="{ achieved: milestone.achieved }">
            <div class="milestone-icon">
              <el-icon :size="24"><Trophy /></el-icon>
            </div>
            <div class="milestone-info">
              <span class="milestone-name">{{ milestone.name }}</span>
              <span class="milestone-desc">{{ milestone.description }}</span>
            </div>
            <el-icon v-if="milestone.achieved" class="check"><CircleCheck /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const posterRef = ref(null)
const trendChartRef = ref(null)

const currentYear = new Date().getFullYear()
const years = Array.from({ length: 5 }, (_, i) => currentYear - i)
// 默认年份跟随当前年，避免跨年后仍停留在硬编码年份
const selectedYear = ref(currentYear)

const userInfo = computed(() => userStore.userInfo || { username: '玩家', avatar: '' })

const reportData = ref({
  totalHours: 0,
  totalGames: 0,
  achievements: 0,
  completedGames: 0,
  favoriteGame: null,
  tags: [],
  genres: [],
  monthlyTrend: [],
  snapshotSource: 'realtime',
  snapshotGeneratedAt: null
})

// 有效快照：来源为 snapshot 且带有生成时间，用于按钮文案与来源提示
const hasSnapshot = computed(() =>
  reportData.value.snapshotSource === 'snapshot' && !!reportData.value.snapshotGeneratedAt
)

// 生成/刷新快照进行中，防止重复提交
const generating = ref(false)

const monthlyData = computed(() => reportData.value.monthlyTrend || [])
const favoriteGame = computed(() => reportData.value.favoriteGame || null)
const reportTags = computed(() => reportData.value.tags || [])
const reportGenres = computed(() => reportData.value.genres || [])

const milestones = computed(() => [
  { id: 1, name: '游戏达人', description: '年度游玩超过300小时', achieved: reportData.value.totalHours >= 300 },
  { id: 2, name: '成就猎人', description: '年度解锁3个成就', achieved: reportData.value.achievements >= 3 },
  { id: 3, name: '通关达人', description: '年度通关1款游戏', achieved: reportData.value.completedGames >= 1 },
  { id: 4, name: '收藏家', description: '年度游玩8款游戏', achieved: reportData.value.totalGames >= 8 },
  { id: 5, name: '传奇玩家', description: '年度游玩超过1000小时', achieved: reportData.value.totalHours >= 1000 }
])

let trendChart = null

// 统一的数据覆盖入口：GET 与生成接口都走这里，保证字段兜底一致
function applyReportData(data) {
  const source = data || {}
  reportData.value = {
    totalHours: source.totalHours ?? 0,
    totalGames: source.totalGames ?? 0,
    achievements: source.achievements ?? 0,
    completedGames: source.completedGames ?? 0,
    favoriteGame: source.favoriteGame || null,
    tags: source.tags || [],
    genres: source.genres || [],
    monthlyTrend: source.monthlyTrend || [],
    snapshotSource: source.snapshotSource === 'snapshot' ? 'snapshot' : 'realtime',
    snapshotGeneratedAt: source.snapshotGeneratedAt || null
  }
}

async function fetchReport() {
  try {
    const response = await api.get('/report/yearly', { params: { year: selectedYear.value } })
    // 空库/空年份/未来年份统一兜底，避免模板解引用 null
    applyReportData(response)
    initTrendChart()
  } catch (e) {
    // error handled by axios interceptor
  }
}

// 显式生成/刷新当前年份快照：成功立即用返回值覆盖页面并重绘，失败保留原数据
async function generateSnapshot() {
  if (generating.value) return
  generating.value = true
  try {
    const response = await api.post('/report/yearly/generate', null, {
      params: { year: selectedYear.value }
    })
    applyReportData(response)
    initTrendChart()
    ElMessage.success(hasSnapshot.value ? '快照已刷新' : '快照已生成')
  } catch (e) {
    ElMessage.error('快照生成失败，已保留原有数据')
  } finally {
    generating.value = false
  }
}

function initTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  const data = (reportData.value.monthlyTrend || []).map(t => t.hours)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 26, 46, 0.9)',
      borderColor: '#27273f',
      textStyle: { color: '#fff' },
      formatter: '{b}: {c} 小时'
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: months,
      axisLine: { lineStyle: { color: '#27273f' } },
      axisLabel: { color: '#71717a' },
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
      barWidth: 20,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#6366f1' },
          { offset: 1, color: '#22d3ee' }
        ])
      }
    }]
  }
  trendChart.setOption(option)
}

function downloadPoster() {
  ElMessage.success('海报已保存到本地')
}

function shareToDiscord() {
  ElMessage.success('已分享到 Discord')
}

watch(selectedYear, () => fetchReport())

onMounted(() => fetchReport())
</script>
<style lang="scss" scoped>
.year-report-page {
  max-width: 800px;
  margin: 0 auto;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  
  h1 {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
  }

  .report-header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .snapshot-tip {
    font-size: 12px;
    color: $primary-color;

    &.realtime {
      color: $text-muted;
    }
  }
}

.report-poster {
  position: relative;
  border-radius: $radius-lg;
  overflow: hidden;
  margin-bottom: 24px;
}

.poster-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a1a3e 0%, #0f0f23 50%, #1a1a2e 100%);
  
  .poster-gradient {
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse at 50% 0%, rgba($primary-color, 0.2) 0%, transparent 60%),
                radial-gradient(ellipse at 100% 100%, rgba($secondary-color, 0.15) 0%, transparent 50%);
  }
}

.poster-content {
  position: relative;
  padding: 40px;
}

.poster-title {
  text-align: center;
  margin-bottom: 32px;
  
  .year {
    display: block;
    font-size: 64px;
    font-weight: 800;
    background: linear-gradient(135deg, $primary-color, $secondary-color);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
  }
  
  .subtitle {
    font-size: 20px;
    color: $text-secondary;
  }
}

.poster-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
  
  .username {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
  }
}

.poster-stats {
  margin-bottom: 32px;
  
  .stat-item.main {
    text-align: center;
    margin-bottom: 20px;
    
    .value {
      display: block;
      font-size: 48px;
      font-weight: 800;
      background: linear-gradient(135deg, $primary-color, $secondary-color);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    
    .label {
      font-size: 14px;
      color: $text-secondary;
    }
  }
  
  .stat-row {
    display: flex;
    justify-content: center;
    gap: 40px;
    
    .stat-item {
      text-align: center;
      
      .value {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: $text-primary;
      }
      
      .label {
        font-size: 12px;
        color: $text-secondary;
      }
    }
  }
}

.poster-favorite {
  position: relative;
  background: rgba($bg-card, 0.5);
  border-radius: $radius-md;
  overflow: hidden;
  padding: 20px;
  margin-bottom: 24px;

  h3 {
    position: relative;
    z-index: 3;
    font-size: 14px;
    color: $text-secondary;
    margin-bottom: 16px;
  }

  .favorite-game {
    position: relative;
    min-height: 200px;
    display: flex;
    align-items: stretch;
  }

  // 无年度最爱时保留同等占位高度，避免海报区塌陷
  .favorite-empty {
    position: relative;
    z-index: 3;
    min-height: 200px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: $text-muted;
  }

  .favorite-cover {
    position: absolute;
    top: 0;
    left: 0;
    width: 66.67%;
    height: 100%;
    overflow: hidden;
    border-radius: 8px 0 0 8px;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &::after {
      content: '';
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      width: 35%;
      background: linear-gradient(to right, transparent 0%, $bg-dark 100%);
      z-index: 1;
      pointer-events: none;
    }
  }

  .game-info {
    position: relative;
    z-index: 2;
    margin-left: calc(66.67% + 20px);
    padding: 24px 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 8px;

    .game-name {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
    }

    .game-hours {
      font-size: 15px;
      color: $primary-color;
    }
  }
}

.poster-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
  
  .tag {
    padding: 6px 16px;
    background: rgba($primary-color, 0.1);
    border: 1px solid rgba($primary-color, 0.3);
    border-radius: 20px;
    font-size: 13px;
    color: $primary-color;
  }
}

.poster-genres {
  margin-bottom: 24px;
  
  h3 {
    font-size: 14px;
    color: $text-secondary;
    margin-bottom: 12px;
  }
}

.genre-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.genre-bar {
  display: grid;
  grid-template-columns: 60px 1fr 40px;
  align-items: center;
  gap: 12px;
  
  .genre-name {
    font-size: 13px;
    color: $text-secondary;
  }
  
  .bar-track {
    height: 8px;
    background: rgba($bg-card, 0.5);
    border-radius: 4px;
    overflow: hidden;
    
    .bar-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.5s ease;
    }
  }
  
  .genre-percent {
    font-size: 12px;
    color: $text-muted;
    text-align: right;
  }
}

.poster-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding-top: 20px;
  border-top: 1px solid rgba($border-color, 0.5);
  
  .brand {
    font-size: 18px;
    font-weight: 700;
    background: linear-gradient(135deg, $primary-color, $secondary-color);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  .slogan {
    font-size: 12px;
    color: $text-muted;
  }
}

.report-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 48px;
}

.report-details {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.detail-section {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 24px;
  
  h3 {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 20px;
  }
}

.chart-container {
  height: 250px;
}

.monthly-ranking {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.month-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: $bg-darker;
  border-radius: $radius-md;
  
  .month-name {
    width: 40px;
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
  }
  
  .month-games {
    flex: 1;
    display: flex;
    gap: 8px;
    
    img {
      width: 36px;
      height: 36px;
      border-radius: 6px;
      object-fit: cover;
    }
  }
  
  .month-hours {
    font-size: 14px;
    font-weight: 500;
    color: $primary-color;
  }
}

.milestones {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.milestone-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: $bg-darker;
  border-radius: $radius-md;
  opacity: 0.5;
  
  &.achieved {
    opacity: 1;
    
    .milestone-icon {
      background: rgba($primary-color, 0.1);
      color: $primary-color;
    }
  }
  
  .milestone-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $bg-card;
    border-radius: $radius-md;
    color: $text-muted;
  }
  
  .milestone-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .milestone-name {
      font-size: 15px;
      font-weight: 500;
      color: $text-primary;
    }
    
    .milestone-desc {
      font-size: 13px;
      color: $text-secondary;
    }
  }
  
  .check {
    color: $success-color;
    font-size: 24px;
  }
}
</style>
