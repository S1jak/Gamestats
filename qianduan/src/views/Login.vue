<template>
  <div class="login-page">
    <div class="login-background">
      <!-- 漩涡螺旋背景：黄金角螺旋臂排布，内圈小淡→外圈大清，层次穿插 -->
      <div class="vortex">
        <div
          v-for="(img, i) in vortexImages"
          :key="i"
          class="vortex-item"
          :class="img.layer"
          :style="{ left: img.left, top: img.top, transform: img.transform, zIndex: img.zIndex }"
        >
          <div
            class="vortex-img"
            :style="{
              backgroundImage: `url('${img.url}')`,
              opacity: img.opacity,
              animationDuration: img.duration,
              animationDelay: img.delay
            }"
          ></div>
        </div>
      </div>
      <!-- 毛玻璃遮罩 + 中心暗化，让前景卡片清晰可读、视觉优先级最高 -->
      <div class="glass-overlay"></div>
    </div>
    
    <div class="login-container">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="48"><Monitor /></el-icon>
        </div>
        <h1 class="title">GameStats</h1>
        <p class="subtitle">你的游戏数据，一目了然</p>
      </div>
      
      <div class="login-card">
        <h2 class="card-title">开始你的游戏之旅</h2>
        <p class="card-desc">登录账号，解锁专属数据分析</p>

        <!-- 手机号密码登录（主登录方式） -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handlePhoneLogin"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="loginForm.phone"
              placeholder="请输入手机号"
              size="large"
              :prefix-icon="Phone"
              maxlength="11"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <div class="form-options">
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
            <el-button text type="primary" class="forget-btn">忘记密码？</el-button>
          </div>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            @click="handlePhoneLogin"
          >
            登 录
          </el-button>
          <div class="register-tip">
            还没有账号？<el-button text type="primary" class="register-btn">立即注册</el-button>
          </div>
        </el-form>

        <!-- 分隔线 -->
        <div class="divider">
          <span class="divider-text">第三方登录</span>
        </div>

        <!-- 三方登录（辅助登录方式） -->
        <div class="oauth-buttons">
          <button class="oauth-btn steam" @click="handleLogin('steam')">
            <svg class="icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12c0 4.84 3.44 8.87 8 9.8V15H8v-3h2V9.5C10 7.57 11.57 6 13.5 6H16v3h-2c-.55 0-1 .45-1 1v2h3l-.5 3H13v6.95c5.05-.5 9-4.76 9-9.95 0-5.52-4.48-10-10-10z"/>
            </svg>
            <span>Steam</span>
          </button>

          <button class="oauth-btn epic" @click="handleLogin('epic')">
            <svg class="icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 3h18v18H3V3zm16 16V5H5v14h14zm-8-2h2v-4h4v-2h-4V7h-2v4H7v2h4v4z"/>
            </svg>
            <span>Epic</span>
          </button>

          <button class="oauth-btn discord" @click="handleLogin('discord')">
            <svg class="icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/>
            </svg>
            <span>Discord</span>
          </button>
        </div>

        <div class="terms">
          <p>登录即表示你同意我们的 <a href="#">服务条款</a> 和 <a href="#">隐私政策</a></p>
        </div>
      </div>
      
      <div class="features">
        <div class="feature-item">
          <el-icon :size="24"><DataAnalysis /></el-icon>
          <span>深度数据分析</span>
        </div>
        <div class="feature-item">
          <el-icon :size="24"><Connection /></el-icon>
          <span>多平台聚合</span>
        </div>
        <div class="feature-item">
          <el-icon :size="24"><Share /></el-icon>
          <span>一键分享</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Phone, Lock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

// 登录页背景：Steam 游戏封面库（横向 460×215 官方宣传图）
// 按自由散落布局取用前 N 张分配到四角/侧边/缝隙槽位
const GAME_COVERS = [
  'https://cdn.steamstatic.com/steam/apps/1245620/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1091500/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1174180/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1145360/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1086940/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/271590/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/990080/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/2358720/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/814380/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/292030/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1888930/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/2050910/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/289070/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/2245480/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/2330780/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/812140/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/413150/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1297650/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/242050/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1559340/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/72850/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1180510/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1237970/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1551360/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/1466860/header.jpg',
  'https://cdn.steamstatic.com/steam/apps/2420110/header.jpg'
]

// ===== 漩涡螺旋布局：黄金角 + 线性半径，图呈螺旋臂向外排布 =====
// 设计要点（在漩涡形状前提下执行层次要求）：
//   - 形状：黄金角 137.508° 生成螺旋臂，切线方向倾斜强化漩涡感
//   - 层次：半径归一化 rNorm 经 smoothstep 映射到 size/opacity，
//           内圈小且淡（≈缝隙点缀）→ 中圈中（≈侧边）→ 外圈大且清（≈四角大图）
//   - 均匀：线性半径（非 sqrt）避免外环聚集、中心空洞
//   - 重心：baseRadius=280 内圈避开登录卡，中心留给登录面板
//   - 密度：取 22 张，控制疏密
const GOLDEN_ANGLE = 137.508
const LAYER_BY_THIRD = ['far', 'mid', 'near'] // 远虚 / 中 / 近清
const LAYER_Z = { far: 1, mid: 2, near: 3 }

const VORTEX_CONFIG = {
  baseRadius: 280,   // 内圈起始半径（避开登录卡，中心留给登录面板）
  maxRadius: 1080,    // 外圈半径（外移覆盖到视口四角与边缘）
  ySquash: 0.6,      // 椭圆压扁：匹配 16:9 横屏，x 到边、y 刚好填满顶底
  minScale: 0.5,     // 内圈最小缩放（中心缝隙小图）
  maxScale: 1.6,     // 外圈最大缩放（对应四角大图）
  minOpacity: 0.4,   // 内圈最淡
  maxOpacity: 0.95   // 外圈最清晰
}

// 构建漩涡：黄金角定角度、线性半径定距，smoothstep 缓动给层次渐变
function buildVortex() {
  const { baseRadius, maxRadius, ySquash, minScale, maxScale, minOpacity, maxOpacity } = VORTEX_CONFIG
  const covers = GAME_COVERS.slice(0, 22) // 取 22 张，避免过密
  const n = covers.length
  const radiusRange = maxRadius - baseRadius
  return covers.map((url, i) => {
    const t = n > 1 ? i / (n - 1) : 0
    const angle = i * GOLDEN_ANGLE
    const radius = baseRadius + t * radiusRange   // 线性半径：径向等距，避免外环聚集
    const rad = angle * Math.PI / 180
    const dx = radius * Math.cos(rad)
    const dy = radius * Math.sin(rad) * ySquash    // 椭圆压扁，适配横向视口
    // 归一化半径 → smoothstep S 曲线：慢起-加速-缓收，层次过渡更自然
    const rNorm = radiusRange > 0 ? (radius - baseRadius) / radiusRange : 0
    const ease = rNorm * rNorm * (3 - 2 * rNorm)
    const scale = minScale + (maxScale - minScale) * ease
    const opacity = minOpacity + (maxOpacity - minOpacity) * ease
    // 切线方向倾斜（螺旋臂走向），强化漩涡旋转感
    const tangent = angle + 90
    // 三层景深：按归一化半径三等分（内→远虚 / 中→中 / 外→近清）
    const depthIdx = rNorm < 0.34 ? 0 : rNorm < 0.67 ? 1 : 2
    const layer = LAYER_BY_THIRD[depthIdx]
    return {
      url,
      left: `calc(50% + ${dx.toFixed(1)}px)`,
      top: `calc(50% + ${dy.toFixed(1)}px)`,
      transform: `translate(-50%, -50%) scale(${scale.toFixed(3)}) rotate(${tangent.toFixed(1)}deg)`,
      zIndex: LAYER_Z[layer],
      opacity: opacity.toFixed(2),
      duration: `${18 + (i % 5) * 2}s`,  // 18-26s 微浮动周期
      delay: `${-(i % 6) * 1.5}s`,        // 错开浮动节奏
      layer
    }
  })
}

const vortexImages = buildVortex()

const loginFormRef = ref(null)
const submitting = ref(false)

const loginForm = reactive({
  phone: '',
  password: '',
  remember: false
})

// 手机号校验
const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const loginRules = {
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ]
}

// 手机号密码登录（主登录方式）
async function handlePhoneLogin() {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await userStore.loginWithPhone(loginForm.phone, loginForm.password)
    ElMessage.success('登录成功')
    // 优先跳转到登录前要访问的页面，默认进入 dashboard
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' ? redirect : '/dashboard')
  } catch (error) {
    // 错误信息已由 axios 拦截器统一提示
    console.error('登录失败', error)
  } finally {
    submitting.value = false
  }
}

// 三方登录（辅助登录方式）
async function handleLogin(provider) {
  try {
    ElMessage.info(`正在跳转到 ${provider} 登录...`)
    await userStore.login(provider)
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  isolation: isolate; // 隔离层叠上下文：让游戏图+遮罩整体位于登录卡片之下
  background: linear-gradient(135deg, $bg-darker 0%, $bg-dark 50%, #1a1a3e 100%);

  // 漩涡容器：满铺背景层，黄金角螺旋排布（单张各自微浮动）
  .vortex {
    position: absolute;
    inset: 0;
  }

  // 单张封面定位点：left/top 由 JS 给出（calc 50% ± 像素偏移），transform 完成居中+缩放+切线倾斜
  .vortex-item {
    position: absolute;

    // 三层景深：按归一化半径三等分——内圈远虚、中圈适中、外圈近清
    &.far  { filter: blur(2.8px) brightness(0.72); }
    &.mid  { filter: blur(1px)   brightness(0.86); }
    &.near { filter: brightness(0.97); }
  }

  // 实际图片盒：基准尺寸 + 微圆角 + 阴影，弱化刺眼感（缩放由外层 transform 控制）
  .vortex-img {
    width: 300px;
    height: 141px; // Steam header 460×215 等比缩放
    background-size: cover;
    background-position: center;
    background-color: rgba($bg-card, 0.2);
    border-radius: 8px;
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.55);
    transform-origin: center;
    animation: vortexFloat 20s ease-in-out infinite; // 微浮动（周期/延迟由内联覆盖）
    will-change: transform, opacity;
  }

  // 毛玻璃遮罩 + 中心暗化（弱化背景、突出登录弹窗）+ 彩色光晕
  .glass-overlay {
    position: absolute;
    inset: 0;
    background:
      radial-gradient(circle at 50% 50%, rgba($bg-darker, 0.72) 0%, rgba($bg-darker, 0.28) 20%, transparent 44%),
      radial-gradient(ellipse at 30% 20%, rgba($primary-color, 0.16) 0%, transparent 55%),
      radial-gradient(ellipse at 70% 80%, rgba($secondary-color, 0.1) 0%, transparent 55%),
      rgba($bg-dark, 0.18);
    backdrop-filter: blur(6px) saturate(120%);
    -webkit-backdrop-filter: blur(6px) saturate(120%);
    z-index: 4;
  }
}

// 单张微浮动：螺旋排布下的轻微上下呼吸
@keyframes vortexFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-14px); }
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.login-header {
  text-align: center;
  
  .logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, $primary-color, $secondary-color);
    border-radius: 20px;
    color: white;
    margin-bottom: 16px;
    box-shadow: 0 8px 32px rgba($primary-color, 0.3);
  }
  
  .title {
    font-size: 36px;
    font-weight: 700;
    background: linear-gradient(135deg, $primary-color, $secondary-color);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 8px;
  }
  
  .subtitle {
    font-size: 16px;
    color: $text-secondary;
  }
}

.login-card {
  background: rgba($bg-card, 0.62);
  backdrop-filter: blur(16px) saturate(125%);
  -webkit-backdrop-filter: blur(16px) saturate(125%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: $radius-lg;
  padding: 32px;
  width: 400px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05);

  .card-title {
    font-size: 20px;
    font-weight: 600;
    color: $text-primary;
    text-align: center;
    margin-bottom: 8px;
  }

  .card-desc {
    font-size: 14px;
    color: $text-secondary;
    text-align: center;
    margin-bottom: 24px;
  }
}

.login-form {
  margin-bottom: 8px;

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-input__wrapper) {
    background-color: $bg-darker !important;
    box-shadow: 0 0 0 1px $border-color inset !important;
  }

  :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px $border-light inset !important;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px $primary-color inset !important;
  }

  :deep(.el-input__inner) {
    height: 44px;
    font-size: 15px;
  }

  :deep(.el-input__prefix) {
    color: $text-muted;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;

  :deep(.el-checkbox__label) {
    color: $text-secondary;
  }

  .forget-btn {
    padding: 6px 12px;
    font-size: 13px;
    border: 1px solid transparent;
    border-radius: $radius-sm;
    transition: all $transition-normal;

    // 悬停效果与 Steam 按钮保持一致
    &:hover {
      transform: translateY(-2px);
      box-shadow: $shadow-md;
      background: #1b2838;
      border-color: #1b2838;
      color: white !important;
    }
  }
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, $primary-color, $secondary-color);
  border: none;

  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: $shadow-glow;
  }
}

.register-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: $text-secondary;

  .register-btn {
    padding: 6px 12px;
    font-size: 13px;
    border: 1px solid transparent;
    border-radius: $radius-sm;
    transition: all $transition-normal;

    // 悬停效果与 Steam 按钮保持一致
    &:hover {
      transform: translateY(-2px);
      box-shadow: $shadow-md;
      background: #1b2838;
      border-color: #1b2838;
      color: white !important;
    }
  }
}

.divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 24px 0 16px;

  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: $border-color;
  }

  .divider-text {
    padding: 0 16px;
    color: $text-muted;
    font-size: 12px;
  }
}

.oauth-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.oauth-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 8px;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  background: $bg-card;
  color: $text-primary;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-normal;

  .icon {
    width: 18px;
    height: 18px;
  }

  // 统一悬停效果：均采用 Steam 按钮的悬停样式
  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
    background: #1b2838;
    border-color: #1b2838;
  }
}

.terms {
  margin-top: 20px;
  text-align: center;
  
  p {
    font-size: 12px;
    color: $text-muted;
    
    a {
      color: $primary-color;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.features {
  display: flex;
  gap: 32px;
  
  .feature-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: $text-secondary;
    font-size: 14px;
    
    .el-icon {
      color: $primary-color;
    }
  }
}
</style>
