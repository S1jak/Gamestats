<template>
  <div class="home-page">
    <div class="hero-section">
      <h1 class="hero-title">
        欢迎来到 <span class="text-gradient">GameStats</span>
      </h1>
      <p class="hero-subtitle">
        聚合你的游戏数据，发现你的游戏习惯
      </p>
      <el-button type="primary" size="large" @click="$router.push('/dashboard')">
        开始探索
        <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
    </div>
    
    <div class="features-grid">
      <div class="feature-card">
        <div class="feature-icon">
          <el-icon :size="32"><DataAnalysis /></el-icon>
        </div>
        <h3>数据可视化</h3>
        <p>通过精美的图表直观展示你的游戏数据</p>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon">
          <el-icon :size="32"><Connection /></el-icon>
        </div>
        <h3>多平台聚合</h3>
        <p>支持 Steam、Epic、PS5、Switch 等主流平台</p>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon">
          <el-icon :size="32"><TrendCharts /></el-icon>
        </div>
        <h3>深度分析</h3>
        <p>分析你的游戏习惯，发现隐藏的游戏偏好</p>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon">
          <el-icon :size="32"><Share /></el-icon>
        </div>
        <h3>社交分享</h3>
        <p>生成年度报告，与好友分享你的游戏成就</p>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  text-align: center;
  padding: 60px 0;
  
  .hero-title {
    font-size: 48px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 16px;
  }
  
  .hero-subtitle {
    font-size: 18px;
    color: $text-secondary;
    margin-bottom: 32px;
  }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 24px;
  padding: 20px 0;
}

.feature-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 32px;
  text-align: center;
  transition: all $transition-normal;
  
  &:hover {
    transform: translateY(-4px);
    border-color: $primary-color;
    box-shadow: $shadow-glow;
    
    .feature-icon {
      background: rgba($primary-color, 0.2);
    }
  }
  
  .feature-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    background: rgba($primary-color, 0.1);
    border-radius: $radius-md;
    color: $primary-color;
    margin-bottom: 20px;
    transition: all $transition-normal;
  }
  
  h3 {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 8px;
  }
  
  p {
    font-size: 14px;
    color: $text-secondary;
    line-height: 1.6;
  }
}
</style>
