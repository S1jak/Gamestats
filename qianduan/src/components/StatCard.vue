<template>
  <div class="stat-card">
    <div class="icon-wrapper" :style="{ background: `${color}20` }">
      <el-icon :size="24" :style="{ color }">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="stat-content">
      <span class="stat-value">{{ value }}</span>
      <span class="stat-title">{{ title }}</span>
    </div>
    <div v-if="trend !== undefined" class="stat-trend" :class="{ up: trend > 0, down: trend < 0 }">
      <el-icon><component :is="trend > 0 ? 'Top' : 'Bottom'" /></el-icon>
      <span>{{ Math.abs(trend) }}%</span>
    </div>
    <div v-if="progress !== undefined" class="stat-progress">
      <el-progress 
        :percentage="progress" 
        :stroke-width="4"
        :show-text="false"
        :color="color"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  icon: {
    type: String,
    required: true
  },
  color: {
    type: String,
    default: '#6366f1'
  },
  trend: {
    type: Number,
    default: undefined
  },
  progress: {
    type: Number,
    default: undefined
  }
})
</script>

<style lang="scss" scoped>
.stat-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all $transition-normal;
  
  &:hover {
    transform: translateY(-2px);
    border-color: $border-light;
    box-shadow: $shadow-md;
  }
}

.icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
  }
  
  .stat-title {
    font-size: 13px;
    color: $text-secondary;
  }
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  
  &.up {
    color: $success-color;
  }
  
  &.down {
    color: $danger-color;
  }
}

.stat-progress {
  margin-top: 4px;
}
</style>
