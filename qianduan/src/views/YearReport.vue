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
    
    <div class="report-poster">
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
      <el-button type="primary" size="large" :loading="exporting" :disabled="exporting" @click="downloadPoster">
        <el-icon><Download /></el-icon>
        保存海报
      </el-button>
      <el-button size="large" :loading="savingShare" :disabled="savingShare" @click="saveShareRecord">
        <el-icon><Collection /></el-icon>
        保存为分享记录
      </el-button>
      <el-button size="large" @click="copyShareText">
        <el-icon><CopyDocument /></el-icon>
        复制分享文案
      </el-button>
      <el-button v-if="canSystemShare" size="large" @click="shareNatively">
        <el-icon><Share /></el-icon>
        系统分享
      </el-button>
    </div>
    <p v-if="shareSaved" class="share-saved-tip">
      已保存到分享记录，<router-link to="/social">去社交中心查看</router-link>
    </p>
    <p class="report-actions-tip">
      海报导出为 1080×1920 PNG 图片；"保存为分享记录"仅同步到社交中心，两者互不影响。导出失败时可使用浏览器截图保存
    </p>
    
    <div class="report-details">
      <div class="detail-section">
        <h3>游戏时长趋势</h3>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      
      <div class="detail-section">
        <h3>月度游戏排行</h3>
        <div class="monthly-ranking">
          <div v-for="month in monthlyData" :key="month.month" class="month-item">
            <span class="month-name">{{ month.month }}月</span>
            <div class="month-games" v-if="month.topGames.length">
              <div
                v-for="game in month.topGames"
                :key="game.id"
                class="month-game"
                :title="game.name"
              >
                <img
                  v-if="game.cover && !isCoverFailed(month, game)"
                  :src="game.cover"
                  :alt="game.name"
                  @error="markCoverFailed(month, game)"
                />
                <div v-else class="month-cover-placeholder">{{ (game.name || '?').charAt(0) }}</div>
                <span class="month-game-hours">{{ formatDecimal(game.hours, preferencesStore.state.showDecimal) }}h</span>
              </div>
            </div>
            <div class="month-games month-games-empty" v-else>本月无记录</div>
            <span class="month-hours">{{ formatHours(month.hours) }}h</span>
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
import { usePreferencesStore } from '@/store/preferences'
import { formatDecimal } from '@/utils/date'
import { exportYearReportPoster, downloadBlobUrl } from '@/utils/poster'

const userStore = useUserStore()
const preferencesStore = usePreferencesStore()
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

// 海报导出进行中，防止重复点击产生多个下载
const exporting = ref(false)

// 分享文案与系统分享能力（navigator.share 不存在时不渲染按钮）
const shareText = computed(() => {
  const hours = Number(reportData.value.totalHours) || 0
  const games = Number(reportData.value.totalGames) || 0
  return `${selectedYear.value} 年度游戏总结 · ${hours} 小时 · ${games} 款游戏`
})
const canSystemShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function'

// 保存为分享记录（POST /api/social/shares），失败不谎报成功
const savingShare = ref(false)
const shareSaved = ref(false)

function resetShareState() {
  shareSaved.value = false
}

async function saveShareRecord() {
  if (savingShare.value) return
  savingShare.value = true
  try {
    await api.post('/social/shares', {
      type: 'year_report',
      refId: selectedYear.value,
      title: `${selectedYear.value} 年度报告`,
      summary: `${reportData.value.totalHours ?? 0} 小时 · ${reportData.value.totalGames ?? 0} 款游戏`,
      visibility: 'friends'
    })
    shareSaved.value = true
    ElMessage.success('已保存到分享记录')
  } catch (e) {
    // 错误 message 由 api.js 统一提示
  } finally {
    savingShare.value = false
  }
}

// 月度排行：topGames 已由后端按小时降序返回（≤3 项），空月为空数组
const monthlyData = computed(() =>
  (reportData.value.monthlyTrend || []).map((item) => ({
    month: item.month,
    hours: item.hours ?? 0,
    topGames: (item.topGames || []).map((game) => ({
      id: game.id,
      name: game.name || '',
      cover: game.cover || '',
      hours: game.hours ?? 0
    }))
  }))
)

// 封面加载失败记录：以"月份-游戏id"为键，避免同一游戏跨月串味
const failedCovers = ref({})

function isCoverFailed(month, game) {
  return !!failedCovers.value[`${month.month}-${game.id}`]
}

function markCoverFailed(month, game) {
  failedCovers.value[`${month.month}-${game.id}`] = true
}

// 小时保留 1 位小数；异常值兜底为 0.0，不渲染 NaN/undefined
function formatHours(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(1) : '0.0'
}

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
  // 切换年份后重新尝试加载封面，避免上一轮失败记录残留
  failedCovers.value = {}
  reportData.value = {
    totalHours: source.totalHours ?? 0,
    totalGames: source.totalGames ?? 0,
    achievements: source.achievements ?? 0,
    completedGames: source.completedGames ?? 0,
    favoriteGame: source.favoriteGame || null,
    tags: source.tags || [],
    genres: source.genres || [],
    // 逐个补 topGames=[]，兼容旧快照/异常响应，保证模板不塌陷
    monthlyTrend: (Array.isArray(source.monthlyTrend) ? source.monthlyTrend : []).map((item) => ({
      month: item?.month,
      hours: item?.hours ?? 0,
      topGames: Array.isArray(item?.topGames) ? item.topGames : []
    })),
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

// Canvas 1080×1920 真实导出：失败一律提示"导出失败，可截图保存"，不谎报成功
async function downloadPoster() {
  if (exporting.value) return
  exporting.value = true
  let objectUrl = ''
  try {
    const result = await exportYearReportPoster({
      year: selectedYear.value,
      username: userInfo.value.username,
      avatarUrl: userInfo.value.avatar,
      totalHours: reportData.value.totalHours,
      totalGames: reportData.value.totalGames,
      achievements: reportData.value.achievements,
      completedGames: reportData.value.completedGames,
      favoriteGame: reportData.value.favoriteGame,
      genres: reportData.value.genres,
      tags: reportData.value.tags,
      monthlyTrend: reportData.value.monthlyTrend
    })
    objectUrl = result.blobUrl
    downloadBlobUrl(result.blobUrl, result.fileName)
    ElMessage.success('海报已保存')
  } catch (e) {
    ElMessage.error('导出失败，可截图保存')
  } finally {
    // 延迟释放，避免部分浏览器下载尚未开始时对象 URL 已被回收
    if (objectUrl) {
      setTimeout(() => URL.revokeObjectURL(objectUrl), 10000)
    }
    exporting.value = false
  }
}

// 复制分享文案：优先 Clipboard API，不支持时降级 textarea + execCommand，仍失败则提示手动复制
async function copyShareText() {
  const text = shareText.value
  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.setAttribute('readonly', '')
      textarea.style.position = 'fixed'
      textarea.style.top = '0'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      const copied = document.execCommand && document.execCommand('copy')
      document.body.removeChild(textarea)
      if (!copied) throw new Error('COPY_FAILED')
    }
    ElMessage.success('文案已复制')
  } catch (e) {
    ElMessage.warning(`复制失败，请手动复制：${text}`)
  }
}

// 系统分享：仅浏览器支持时可用（按钮条件渲染），取消分享不提示错误
async function shareNatively() {
  if (!canSystemShare) return
  try {
    await navigator.share({
      title: shareText.value,
      text: shareText.value
    })
  } catch (e) {
    if (e && e.name === 'AbortError') return
    ElMessage.info('系统分享未完成，可先复制分享文案')
  }
}

watch(selectedYear, () => {
  resetShareState()
  fetchReport()
})

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

.report-actions-tip {
  margin: -32px 0 40px;
  text-align: center;
  font-size: 12px;
  color: $text-muted;
}

.share-saved-tip {
  margin: -30px 0 26px;
  text-align: center;
  font-size: 13px;
  color: $success-color;

  a {
    color: $primary-color;
    margin-left: 4px;
  }
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
    align-items: center;
    gap: 8px;
    
    img {
      width: 36px;
      height: 36px;
      border-radius: 6px;
      object-fit: cover;
      flex-shrink: 0;
    }
  }

  // 无封面时渲染渐变占位块（首字母），不生成 <img src=""> 造成裂图/空请求
  .month-cover-placeholder {
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    color: $text-secondary;
    background: linear-gradient(135deg, rgba($primary-color, 0.35), rgba($secondary-color, 0.25));
  }

  .month-game {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .month-game-hours {
    font-size: 12px;
    color: $text-secondary;
    white-space: nowrap;
  }

  // 空月占位：保留与原容器一致的高度，避免列表塌陷
  .month-games-empty {
    font-size: 13px;
    color: $text-muted;
    min-height: 36px;
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
