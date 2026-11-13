﻿﻿﻿﻿﻿﻿﻿<template>
  <div class="game-card" :class="[viewMode]" @click="$emit('click')">
    <div class="card-cover">
      <img :src="game.cover" :alt="game.name" />
      <div class="cover-overlay">
        <el-button type="primary" size="small" circle>
          <el-icon><View /></el-icon>
        </el-button>
      </div>
      <div class="status-badge" :class="statusClass">
        {{ statusText }}
      </div>
    </div>

    <div class="card-content">
      <h3 class="game-name">{{ game.name }}</h3>

      <div class="game-meta">
        <span class="genre">{{ game.genre }}</span>
        <span class="play-time">{{ formatPlayTime(game.playTime) }}</span>
      </div>

      <div class="game-stats">
        <div class="stat">
          <el-icon><Trophy /></el-icon>
          <span>{{ game.achievements }}/{{ game.totalAchievements }}</span>
        </div>
        <div class="stat rating" :class="ratingClass">
          <el-icon><Star /></el-icon>
          <span>{{ game.rating }}</span>
        </div>
      </div>

      <div v-if="viewMode === 'list'" class="list-extra">
        <span class="last-played">最近游玩 {{ game.lastPlayed }}</span>
        <el-dropdown @command="(cmd) => $emit('update-status', game.id, cmd)">
          <el-button size="small" text>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="正在玩">正在玩</el-dropdown-item>
              <el-dropdown-item command="已完成">已完成</el-dropdown-item>
              <el-dropdown-item command="未玩">未玩</el-dropdown-item>
              <el-dropdown-item command="搁置">搁置</el-dropdown-item>
              <el-dropdown-item command="弃坑">弃坑</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  game: { type: Object, required: true },
  viewMode: { type: String, default: 'grid' }
})

defineEmits(['click', 'update-status'])

const statusText = computed(() => props.game.status || '')

const STATUS_CLASS_MAP = {
  '正在玩': 'playing',
  '已完成': 'completed',
  '未玩': 'not_played',
  '搁置': 'on_hold',
  '弃坑': 'dropped'
}
const statusClass = computed(() => STATUS_CLASS_MAP[props.game.status] || 'not_played')

const ratingClass = computed(() => {
  const r = props.game.rating
  if (r >= 9) return 'excellent'
  if (r >= 8) return 'great'
  if (r >= 7) return 'good'
  return 'average'
})

function formatPlayTime(m) {
  if (m < 60) return m + '分钟'
  const h = Math.floor(m / 60)
  if (h < 24) return h + '小时'
  return Math.floor(h / 24) + '天' + (h % 24) + '时'
}
</script>

<style lang="scss" scoped>
.game-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  overflow: hidden;
  cursor: pointer;
  transition: all $transition-normal;

  &:hover {
    transform: translateY(-4px);
    border-color: $border-light;
    box-shadow: $shadow-lg;

    .cover-overlay { opacity: 1; }
  }

  &.list {
    position: relative;
    border-radius: $radius-sm;
    min-height: 300px;

    &:hover {
      transform: translateY(-2px);
    }

    .card-cover {
      position: absolute;
      top: 0;
      left: 0;
      width: 66.67%;
      height: 100%;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform $transition-normal;
      }

      &:hover img {
        transform: scale(1.05);
      }

      &::after {
        content: '';
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        width: 35%;
        background: linear-gradient(to right, transparent 0%, $bg-card 100%);
        z-index: 1;
        pointer-events: none;
      }
    }

    .cover-overlay { display: none; }

    .status-badge { z-index: 2; }

    .card-content {
      position: relative;
      z-index: 2;
      margin-left: calc(66.67% + 20px);
      padding: 24px 24px 24px 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 10px;
      min-height: 300px;
    }

    .game-name {
      font-size: 20px;
      margin-bottom: 0;
      white-space: normal;
    }

    .game-meta {
      margin-bottom: 0;
      flex-direction: row;
      gap: 12px;

      .genre { font-size: 14px; }
      .play-time { font-size: 14px; }
    }

    .game-stats {
      justify-content: flex-start;
      gap: 24px;

      .stat {
        font-size: 14px;
        .el-icon { font-size: 16px; }
      }
    }

    .list-extra {
      margin-top: 8px;
      padding-top: 10px;
      border-top: 1px solid $border-color;

      .last-played { font-size: 13px; }
    }
  }
}

.card-cover {
  position: relative;
  height: 280px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-normal;
  }

  &:hover img { transform: scale(1.05); }
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba($bg-dark, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity $transition-fast;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;

  &.playing { background: $primary-color; color: white; }
  &.completed { background: $success-color; color: white; }
  &.not_played { background: $bg-card; color: $text-secondary; }
  &.on_hold { background: $warning-color; color: white; }
  &.dropped { background: $text-muted; color: white; }
}

.card-content { padding: 16px; }

.game-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.game-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;

  .genre {
    font-size: 12px;
    color: $primary-color;
    background: rgba($primary-color, 0.1);
    padding: 2px 8px;
    border-radius: 4px;
  }

  .play-time {
    font-size: 12px;
    color: $text-secondary;
  }
}

.game-stats {
  display: flex;
  justify-content: space-between;

  .stat {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: $text-secondary;
    .el-icon { font-size: 14px; }

    &.rating {
      &.excellent { color: $success-color; }
      &.great { color: $primary-color; }
      &.good { color: $warning-color; }
      &.average { color: $text-muted; }
    }
  }
}

.list-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid $border-color;

  .last-played {
    font-size: 12px;
    color: $text-muted;
  }
}
</style>
