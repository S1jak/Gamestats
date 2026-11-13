<template>
  <div class="social-page">
    <div class="page-header">
      <h2>社交中心</h2>
      <el-button type="primary" @click="showInviteDialog = true">
        <el-icon><Plus /></el-icon>
        添加好友
      </el-button>
    </div>
    
    <div class="tabs-container">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="好友列表" name="friends">
          <div class="friends-grid">
            <div 
              v-for="friend in friends" 
              :key="friend.id" 
              class="friend-card"
            >
              <div class="friend-header">
                <el-avatar :size="48" :src="friend.avatar">
                  {{ friend.username.charAt(0) }}
                </el-avatar>
                <div class="friend-info">
                  <span class="friend-name">{{ friend.username }}</span>
                  <span class="friend-status" :class="{ online: friend.online }">
                    {{ friend.online ? '在线' : '离线' }}
                  </span>
                </div>
                <el-dropdown>
                  <el-button text>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="compareWith(friend)">数据对比</el-dropdown-item>
                      <el-dropdown-item>发送消息</el-dropdown-item>
                      <el-dropdown-item divided>删除好友</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              
              <div class="friend-stats">
                <div class="stat">
                  <span class="value">{{ friend.totalGames }}</span>
                  <span class="label">游戏</span>
                </div>
                <div class="stat">
                  <span class="value">{{ friend.playTime }}h</span>
                  <span class="label">时长</span>
                </div>
                <div class="stat">
                  <span class="value">{{ friend.achievements }}</span>
                  <span class="label">成就</span>
                </div>
              </div>
              
              <div class="recent-games">
                <span class="label">最近游玩</span>
                <div class="game-icons">
                  <img 
                    v-for="game in friend.recentGames" 
                    :key="game.id" 
                    :src="game.cover" 
                    :alt="game.name"
                  />
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="数据对比" name="compare">
          <div class="compare-section">
            <div class="compare-header">
              <div class="player-select">
                <el-select v-model="comparePlayer1" placeholder="选择玩家">
                  <el-option 
                    v-for="friend in friends" 
                    :key="friend.id" 
                    :label="friend.username" 
                    :value="friend.id"
                  />
                </el-select>
              </div>
              <span class="vs">VS</span>
              <div class="player-select">
                <el-select v-model="comparePlayer2" placeholder="选择玩家">
                  <el-option label="我" value="me" />
                  <el-option 
                    v-for="friend in friends" 
                    :key="friend.id" 
                    :label="friend.username" 
                    :value="friend.id"
                  />
                </el-select>
              </div>
            </div>
            
            <div v-if="compareData" class="compare-results">
              <div class="compare-stat" v-for="stat in compareStats" :key="stat.key">
                <div class="stat-bar">
                  <div class="bar-item player1" :style="{ width: stat.player1Percent + '%' }">
                    <span class="value">{{ stat.player1Value }}</span>
                  </div>
                  <div class="bar-item player2" :style="{ width: stat.player2Percent + '%' }">
                    <span class="value">{{ stat.player2Value }}</span>
                  </div>
                </div>
                <span class="stat-label">{{ stat.label }}</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="分享记录" name="shares">
          <div class="shares-grid">
            <div v-for="share in shares" :key="share.id" class="share-card">
              <img :src="share.image" :alt="share.title" class="share-image" />
              <div class="share-content">
                <h4>{{ share.title }}</h4>
                <p>{{ share.description }}</p>
                <div class="share-meta">
                  <span class="date">{{ share.date }}</span>
                  <div class="actions">
                    <el-button text size="small">
                      <el-icon><Download /></el-icon>
                    </el-button>
                    <el-button text size="small">
                      <el-icon><Share /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <el-dialog v-model="showInviteDialog" title="添加好友" width="400px">
      <el-input 
        v-model="inviteCode" 
        placeholder="输入好友邀请码"
        size="large"
      />
      <p class="invite-hint">或分享你的邀请码: <strong>GS-{{ myInviteCode }}</strong></p>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="sendInvite">发送邀请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('friends')
const showInviteDialog = ref(false)
const inviteCode = ref('')
const myInviteCode = ref('A8X9K2')
const comparePlayer1 = ref('')
const comparePlayer2 = ref('me')

const friends = ref([
  { 
    id: 1, 
    username: 'ShadowHunter', 
    avatar: 'https://picsum.photos/seed/f1/100/100',
    online: true,
    totalGames: 89,
    playTime: 2340,
    achievements: 456,
    recentGames: [
      { id: 1, name: '艾尔登法环', cover: 'https://picsum.photos/seed/g1/40/40' },
      { id: 2, name: '博德之门3', cover: 'https://picsum.photos/seed/g2/40/40' }
    ]
  },
  { 
    id: 2, 
    username: 'NightBlade', 
    avatar: 'https://picsum.photos/seed/f2/100/100',
    online: false,
    totalGames: 156,
    playTime: 4520,
    achievements: 892,
    recentGames: [
      { id: 3, name: '赛博朋克2077', cover: 'https://picsum.photos/seed/g3/40/40' }
    ]
  },
  { 
    id: 3, 
    username: 'StarGazer', 
    avatar: 'https://picsum.photos/seed/f3/100/100',
    online: true,
    totalGames: 67,
    playTime: 1890,
    achievements: 234,
    recentGames: [
      { id: 4, name: '星空', cover: 'https://picsum.photos/seed/g4/40/40' },
      { id: 5, name: '无人深空', cover: 'https://picsum.photos/seed/g5/40/40' }
    ]
  }
])

const shares = ref([
  {
    id: 1,
    title: '2024年度游戏总结',
    description: '今年我最爱的游戏是艾尔登法环...',
    image: 'https://picsum.photos/seed/share1/400/200',
    date: '2024-01-01'
  },
  {
    id: 2,
    title: '成就达成：全成就大师',
    description: '恭喜达成100个全成就游戏！',
    image: 'https://picsum.photos/seed/share2/400/200',
    date: '2024-01-15'
  }
])

const compareData = computed(() => {
  return comparePlayer1.value && comparePlayer2.value
})

const compareStats = computed(() => {
  return [
    { key: 'games', label: '游戏数量', player1Value: 89, player2Value: 156, player1Percent: 36, player2Percent: 64 },
    { key: 'hours', label: '游玩时长', player1Value: '2340h', player2Value: '3120h', player1Percent: 43, player2Percent: 57 },
    { key: 'achievements', label: '成就数量', player1Value: 456, player2Value: 892, player1Percent: 34, player2Percent: 66 }
  ]
})

function compareWith(friend) {
  comparePlayer1.value = friend.id
  activeTab.value = 'compare'
}

function sendInvite() {
  if (inviteCode.value.trim()) {
    ElMessage.success('好友邀请已发送')
    showInviteDialog.value = false
    inviteCode.value = ''
  }
}
</script>

<style lang="scss" scoped>
.social-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  h2 {
    font-size: 24px;
    font-weight: 600;
    color: $text-primary;
  }
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.friend-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  padding: 20px;
  transition: all $transition-normal;
  
  &:hover {
    border-color: $border-light;
    transform: translateY(-2px);
  }
}

.friend-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.friend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .friend-name {
    font-size: 15px;
    font-weight: 500;
    color: $text-primary;
  }
  
  .friend-status {
    font-size: 12px;
    color: $text-muted;
    
    &.online {
      color: $success-color;
    }
  }
}

.friend-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  border-top: 1px solid $border-color;
  border-bottom: 1px solid $border-color;
  margin-bottom: 16px;
  
  .stat {
    text-align: center;
    
    .value {
      display: block;
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

.recent-games {
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  .label {
    font-size: 12px;
    color: $text-muted;
  }
  
  .game-icons {
    display: flex;
    gap: 8px;
    
    img {
      width: 36px;
      height: 36px;
      border-radius: 6px;
      object-fit: cover;
    }
  }
}

.compare-section {
  padding: 20px 0;
}

.compare-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 40px;
  
  .vs {
    font-size: 24px;
    font-weight: 700;
    color: $primary-color;
  }
  
  .player-select {
    width: 200px;
  }
}

.compare-results {
  max-width: 600px;
  margin: 0 auto;
}

.compare-stat {
  margin-bottom: 24px;
  
  .stat-label {
    display: block;
    text-align: center;
    font-size: 13px;
    color: $text-secondary;
    margin-top: 8px;
  }
}

.stat-bar {
  display: flex;
  height: 40px;
  border-radius: $radius-md;
  overflow: hidden;
  background: $bg-darker;
  
  .bar-item {
    display: flex;
    align-items: center;
    padding: 0 16px;
    min-width: 80px;
    transition: width 0.5s ease;
    
    .value {
      font-size: 14px;
      font-weight: 600;
      color: white;
    }
    
    &.player1 {
      background: $primary-color;
      justify-content: flex-start;
    }
    
    &.player2 {
      background: $secondary-color;
      justify-content: flex-end;
    }
  }
}

.shares-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.share-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-lg;
  overflow: hidden;
  transition: all $transition-normal;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
  }
  
  .share-image {
    width: 100%;
    height: 160px;
    object-fit: cover;
  }
  
  .share-content {
    padding: 16px;
    
    h4 {
      font-size: 15px;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: 8px;
    }
    
    p {
      font-size: 13px;
      color: $text-secondary;
      margin-bottom: 12px;
    }
  }
  
  .share-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .date {
      font-size: 12px;
      color: $text-muted;
    }
    
    .actions {
      display: flex;
      gap: 4px;
    }
  }
}

.invite-hint {
  margin-top: 16px;
  font-size: 13px;
  color: $text-secondary;
  text-align: center;
  
  strong {
    color: $primary-color;
    font-family: monospace;
  }
}
</style>
