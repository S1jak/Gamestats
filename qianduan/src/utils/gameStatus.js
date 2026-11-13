// 游戏状态唯一枚举：与后端 GameStatusConstant 的中文五值保持一致
export const GAME_STATUS_OPTIONS = [
  { label: '在玩', value: '在玩' },
  { label: '已完成', value: '已完成' },
  { label: '未玩', value: '未玩' },
  { label: '搁置', value: '搁置' },
  { label: '弃坑', value: '弃坑' }
]

// 状态中文值 → 徽标 CSS 类名（保留原有英文类名，供样式复用）
export const GAME_STATUS_CLASS_MAP = {
  '在玩': 'playing',
  '已完成': 'completed',
  '未玩': 'not_played',
  '搁置': 'on_hold',
  '弃坑': 'dropped'
}