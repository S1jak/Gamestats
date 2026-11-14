/**
 * 年度报告海报导出器（零新增依赖，纯 Canvas 1080×1920）
 *
 * 设计约束：
 *   1) 图片一律 crossOrigin='anonymous'，加载失败/超时返回 null，降级为文字/首字母，不阻断导出；
 *   2) document.fonts.ready 带超时（默认 1500ms），超时后使用系统字体栈；
 *   3) toBlob 失败（含跨域污染导致的 SecurityError）时，用纯文字版重绘并重试一次；
 *   4) 仍失败统一抛出 POSTER_EXPORT_FAILED，由调用方提示“导出失败，可截图保存”；
 *   5) 所有数值经 Number 归一，禁止 undefined/NaN 上画布；空库可正常导出（0/暂无）。
 */

export const POSTER_WIDTH = 1080
export const POSTER_HEIGHT = 1920

const FONT_STACK =
  '"Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", "Source Han Sans SC", "Noto Sans CJK SC", sans-serif'

const PALETTE = {
  text: '#f4f4f5',
  textSecondary: '#c7c7d1',
  muted: '#8b8b96',
  primary: '#6366f1',
  secondary: '#22d3ee',
  card: 'rgba(255, 255, 255, 0.05)',
  cardBorder: 'rgba(255, 255, 255, 0.12)',
  track: 'rgba(255, 255, 255, 0.08)'
}

const GENRE_FALLBACK_COLORS = ['#6366f1', '#22d3ee', '#a855f7', '#f472b6', '#34d399', '#fbbf24']
const SAFE_COLOR_RE = /^(#[0-9a-f]{3,8}|rgba?\([\d\s.,%]+\)|hsla?\([\d\s.,%]+\))$/i

function toNumber(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

function toPositiveInt(value) {
  return Math.max(0, Math.round(toNumber(value)))
}

function formatNumber(value) {
  return String(toPositiveInt(value))
}

function formatPercent(value) {
  const num = Math.max(0, Math.min(100, toNumber(value)))
  return `${num.toFixed(1).replace(/\.0$/, '')}%`
}

function safeColor(value, fallback) {
  if (typeof value !== 'string') return fallback
  const color = value.trim()
  return SAFE_COLOR_RE.test(color) ? color : fallback
}

function setFont(ctx, size, weight = '400') {
  ctx.font = `${weight} ${size}px ${FONT_STACK}`
}

/**
 * 在已设置 font 的前提下按像素宽度截断文本，超出部分以省略号收尾。
 */
function fitText(ctx, text, maxWidth) {
  const raw = String(text ?? '').trim()
  if (!raw) return ''
  if (ctx.measureText(raw).width <= maxWidth) return raw
  let low = 1
  let high = raw.length
  while (low < high) {
    const mid = Math.ceil((low + high) / 2)
    if (ctx.measureText(raw.slice(0, mid) + '…').width <= maxWidth) {
      low = mid
    } else {
      high = mid - 1
    }
  }
  return raw.slice(0, low) + '…'
}

function roundRectPath(ctx, x, y, width, height, radius) {
  const r = Math.max(0, Math.min(radius, width / 2, height / 2))
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + width, y, x + width, y + height, r)
  ctx.arcTo(x + width, y + height, x, y + height, r)
  ctx.arcTo(x, y + height, x, y, r)
  ctx.arcTo(x, y, x + width, y, r)
  ctx.closePath()
}

function fillRoundRect(ctx, x, y, width, height, radius, fill, stroke) {
  ctx.save()
  roundRectPath(ctx, x, y, width, height, radius)
  ctx.fillStyle = fill
  ctx.fill()
  if (stroke) {
    ctx.strokeStyle = stroke
    ctx.lineWidth = 2
    ctx.stroke()
  }
  ctx.restore()
}

function drawGradientText(ctx, text, x, y, size, weight, colors, align = 'left') {
  ctx.save()
  setFont(ctx, size, weight)
  ctx.textAlign = align
  ctx.textBaseline = 'alphabetic'
  const width = ctx.measureText(text).width
  const startX = align === 'center' ? x - width / 2 : align === 'right' ? x - width : x
  const gradient = ctx.createLinearGradient(startX, y - size, startX + width, y)
  gradient.addColorStop(0, colors[0])
  gradient.addColorStop(1, colors[1])
  ctx.fillStyle = gradient
  ctx.fillText(text, x, y)
  ctx.restore()
}

function drawDivider(ctx, y) {
  const gradient = ctx.createLinearGradient(72, 0, POSTER_WIDTH - 72, 0)
  gradient.addColorStop(0, 'rgba(99, 102, 241, 0)')
  gradient.addColorStop(0.5, 'rgba(99, 102, 241, 0.6)')
  gradient.addColorStop(1, 'rgba(34, 211, 238, 0)')
  ctx.fillStyle = gradient
  ctx.fillRect(72, y, POSTER_WIDTH - 144, 2)
}

function createPosterModel(payload = {}) {
  const favorite = payload.favoriteGame || {}
  const favoriteName = String(favorite.name ?? '').trim()

  const rawGenres = Array.isArray(payload.genres) ? payload.genres : []
  const genres = rawGenres
    .filter((item) => item && String(item.name ?? '').trim())
    .slice(0, 5)
    .map((item, index) => ({
      name: String(item.name).trim(),
      percent: Math.max(0, Math.min(100, toNumber(item.percent))),
      color: safeColor(item.color, GENRE_FALLBACK_COLORS[index % GENRE_FALLBACK_COLORS.length])
    }))

  const rawMonthly = Array.isArray(payload.monthlyTrend) ? payload.monthlyTrend : []
  const monthlyTrend = Array.from({ length: 12 }, (_, index) => {
    const item = rawMonthly[index] || {}
    const month = Math.min(12, Math.max(1, Math.round(toNumber(item.month, index + 1))))
    return {
      month,
      hours: Math.max(0, toNumber(item.hours))
    }
  })

  let peak = null
  for (const item of monthlyTrend) {
    if (item.hours <= 0) continue
    if (!peak || item.hours > peak.hours) peak = { month: item.month, hours: item.hours }
  }

  return {
    year: Math.round(toNumber(payload.year, new Date().getFullYear())),
    username: String(payload.username ?? '').trim() || '玩家',
    playStyle: String(payload.playStyle ?? '').trim(),
    avatarUrl: typeof payload.avatarUrl === 'string' ? payload.avatarUrl : '',
    totalHours: toPositiveInt(payload.totalHours),
    totalGames: toPositiveInt(payload.totalGames),
    achievements: toPositiveInt(payload.achievements),
    completedGames: toPositiveInt(payload.completedGames),
    favoriteGame: favoriteName
      ? {
          name: favoriteName,
          cover: typeof favorite.cover === 'string' ? favorite.cover : '',
          hours: toPositiveInt(favorite.hours)
        }
      : null,
    tags: (Array.isArray(payload.tags) ? payload.tags : [])
      .filter((tag) => typeof tag === 'string' && tag.trim())
      .slice(0, 3)
      .map((tag) => tag.trim()),
    genres,
    monthlyTrend,
    peak
  }
}

function waitForFonts(timeout = 1500) {
  return new Promise((resolve) => {
    if (typeof document === 'undefined' || !document.fonts || !document.fonts.ready) {
      resolve()
      return
    }
    let settled = false
    const done = () => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      resolve()
    }
    const timer = setTimeout(done, timeout)
    try {
      Promise.resolve(document.fonts.ready).then(done, done)
    } catch (error) {
      done()
    }
  })
}

/**
 * 安全加载图片：crossOrigin=anonymous + 超时；任何失败都返回 null，不抛异常。
 */
function loadImage(url, timeout = 8000) {
  return new Promise((resolve) => {
    const source = typeof url === 'string' ? url.trim() : ''
    if (!source || typeof Image === 'undefined') {
      resolve(null)
      return
    }
    let settled = false
    const done = (image) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      resolve(image || null)
    }
    const timer = setTimeout(() => done(null), timeout)
    try {
      const image = new Image()
      image.crossOrigin = 'anonymous'
      image.referrerPolicy = 'no-referrer'
      image.decoding = 'async'
      image.onload = () => done(image)
      image.onerror = () => done(null)
      image.src = source
    } catch (error) {
      done(null)
    }
  })
}

function loadPosterImages(model) {
  return Promise.all([
    loadImage(model.avatarUrl),
    model.favoriteGame && model.favoriteGame.cover ? loadImage(model.favoriteGame.cover) : Promise.resolve(null)
  ]).then(([avatar, cover]) => ({ avatar, cover }))
}

function createCanvasElement() {
  if (typeof document === 'undefined' || typeof document.createElement !== 'function') return null
  const canvas = document.createElement('canvas')
  if (!canvas || typeof canvas.getContext !== 'function') return null
  canvas.width = POSTER_WIDTH
  canvas.height = POSTER_HEIGHT
  const ctx = canvas.getContext('2d')
  if (!ctx) return null
  return { canvas, ctx }
}

function drawBackground(ctx) {
  const linear = ctx.createLinearGradient(0, 0, POSTER_WIDTH, POSTER_HEIGHT)
  linear.addColorStop(0, '#1a1a3e')
  linear.addColorStop(0.5, '#0f0f23')
  linear.addColorStop(1, '#1a1a2e')
  ctx.fillStyle = linear
  ctx.fillRect(0, 0, POSTER_WIDTH, POSTER_HEIGHT)

  const topGlow = ctx.createRadialGradient(POSTER_WIDTH / 2, 0, 0, POSTER_WIDTH / 2, 0, 760)
  topGlow.addColorStop(0, 'rgba(99, 102, 241, 0.30)')
  topGlow.addColorStop(1, 'rgba(99, 102, 241, 0)')
  ctx.fillStyle = topGlow
  ctx.fillRect(0, 0, POSTER_WIDTH, 900)

  const bottomGlow = ctx.createRadialGradient(POSTER_WIDTH, POSTER_HEIGHT, 0, POSTER_WIDTH, POSTER_HEIGHT, 760)
  bottomGlow.addColorStop(0, 'rgba(34, 211, 238, 0.18)')
  bottomGlow.addColorStop(1, 'rgba(34, 211, 238, 0)')
  ctx.fillStyle = bottomGlow
  ctx.fillRect(POSTER_WIDTH - 800, POSTER_HEIGHT - 900, 800, 900)
}

function drawImageCover(ctx, image, x, y, width, height) {
  if (!image) return false
  const iw = image.width || image.naturalWidth || 0
  const ih = image.height || image.naturalHeight || 0
  if (!iw || !ih) return false
  const scale = Math.max(width / iw, height / ih)
  const drawWidth = iw * scale
  const drawHeight = ih * scale
  try {
    ctx.drawImage(image, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight)
    return true
  } catch (error) {
    return false
  }
}

function drawAvatar(ctx, image, initial, x, y, size) {
  ctx.save()
  ctx.beginPath()
  ctx.arc(x + size / 2, y + size / 2, size / 2, 0, Math.PI * 2)
  ctx.closePath()
  ctx.clip()

  const drawn = image
    ? (() => {
        const iw = image.width || image.naturalWidth || 0
        const ih = image.height || image.naturalHeight || 0
        if (!iw || !ih) return false
        const scale = Math.max(size / iw, size / ih)
        const drawWidth = iw * scale
        const drawHeight = ih * scale
        try {
          ctx.drawImage(image, x + (size - drawWidth) / 2, y + (size - drawHeight) / 2, drawWidth, drawHeight)
          return true
        } catch (error) {
          return false
        }
      })()
    : false

  if (!drawn) {
    const gradient = ctx.createLinearGradient(x, y, x + size, y + size)
    gradient.addColorStop(0, 'rgba(99, 102, 241, 0.92)')
    gradient.addColorStop(1, 'rgba(34, 211, 238, 0.78)')
    ctx.fillStyle = gradient
    ctx.fillRect(x, y, size, size)
    ctx.fillStyle = '#ffffff'
    setFont(ctx, Math.round(size * 0.4), '700')
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(initial || '?', x + size / 2, y + size / 2 + 2)
  }
  ctx.restore()

  ctx.save()
  ctx.beginPath()
  ctx.arc(x + size / 2, y + size / 2, size / 2 - 1, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.18)'
  ctx.lineWidth = 2
  ctx.stroke()
  ctx.restore()
}

function drawCoverThumb(ctx, image, initial, x, y, width, height, radius) {
  ctx.save()
  roundRectPath(ctx, x, y, width, height, radius)
  ctx.clip()
  const drawn = drawImageCover(ctx, image, x, y, width, height)
  if (!drawn) {
    const gradient = ctx.createLinearGradient(x, y, x + width, y + height)
    gradient.addColorStop(0, 'rgba(99, 102, 241, 0.65)')
    gradient.addColorStop(1, 'rgba(34, 211, 238, 0.45)')
    ctx.fillStyle = gradient
    ctx.fillRect(x, y, width, height)
    ctx.fillStyle = 'rgba(255, 255, 255, 0.85)'
    setFont(ctx, Math.round(Math.min(width, height) * 0.34), '700')
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(initial || '?', x + width / 2, y + height / 2 + 2)
  }
  ctx.restore()
}

function drawHeader(ctx) {
  drawGradientText(ctx, 'GameStats', 72, 108, 34, '700', ['#818cf8', '#22d3ee'], 'left')
  ctx.save()
  setFont(ctx, 24, '500')
  ctx.textAlign = 'right'
  ctx.textBaseline = 'alphabetic'
  ctx.fillStyle = PALETTE.muted
  ctx.fillText('年度报告 · ANNUAL REPORT', POSTER_WIDTH - 72, 106)
  ctx.restore()
}

function drawTitle(ctx, model) {
  drawGradientText(ctx, String(model.year), POSTER_WIDTH / 2, 272, 120, '800', ['#818cf8', '#22d3ee'], 'center')
  ctx.save()
  setFont(ctx, 40, '500')
  ctx.textAlign = 'center'
  ctx.textBaseline = 'alphabetic'
  ctx.fillStyle = PALETTE.textSecondary
  ctx.fillText('年度游戏总结', POSTER_WIDTH / 2, 330)
  ctx.restore()
}

function drawIdentity(ctx, model, avatar) {
  drawAvatar(ctx, avatar, model.username.charAt(0), 72, 398, 96)

  ctx.save()
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'
  setFont(ctx, 38, '600')
  ctx.fillStyle = PALETTE.text
  ctx.fillText(fitText(ctx, model.username, 660), 196, 448)

  if (model.playStyle) {
    setFont(ctx, 24, '400')
    ctx.fillStyle = PALETTE.muted
    ctx.fillText(fitText(ctx, model.playStyle, 660), 196, 486)
  }
  ctx.restore()
}

function drawMainStats(ctx, model) {
  fillRoundRect(ctx, 72, 530, POSTER_WIDTH - 144, 210, 28, PALETTE.card, PALETTE.cardBorder)

  ctx.save()
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'
  setFont(ctx, 26, '500')
  ctx.fillStyle = PALETTE.muted
  ctx.fillText('本年度游戏时长', 104, 592)

  const hoursText = formatNumber(model.totalHours)
  setFont(ctx, 96, '800')
  const hoursWidth = ctx.measureText(hoursText).width
  drawGradientText(ctx, hoursText, 104, 706, 96, '800', ['#818cf8', '#22d3ee'], 'left')

  setFont(ctx, 32, '500')
  ctx.fillStyle = PALETTE.textSecondary
  ctx.fillText('小时', 104 + hoursWidth + 18, 706)
  ctx.restore()
}

function drawSubStats(ctx, model) {
  const items = [
    { value: model.totalGames, label: '款游戏' },
    { value: model.achievements, label: '个成就' },
    { value: model.completedGames, label: '款通关' }
  ]
  const columnWidth = (POSTER_WIDTH - 144) / 3

  ctx.save()
  ctx.textAlign = 'center'
  ctx.textBaseline = 'alphabetic'
  items.forEach((item, index) => {
    const centerX = 72 + columnWidth * (index + 0.5)
    setFont(ctx, 56, '700')
    ctx.fillStyle = PALETTE.text
    ctx.fillText(formatNumber(item.value), centerX, 830)
    setFont(ctx, 24, '400')
    ctx.fillStyle = PALETTE.muted
    ctx.fillText(item.label, centerX, 868)
  })
  ctx.restore()
}

function drawFavorite(ctx, model, cover) {
  fillRoundRect(ctx, 72, 915, POSTER_WIDTH - 144, 300, 28, PALETTE.card, PALETTE.cardBorder)

  ctx.save()
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'
  setFont(ctx, 26, '500')
  ctx.fillStyle = PALETTE.muted
  ctx.fillText('年度最爱', 104, 966)

  const favorite = model.favoriteGame
  if (!favorite) {
    ctx.textAlign = 'center'
    setFont(ctx, 40, '600')
    ctx.fillStyle = PALETTE.textSecondary
    ctx.fillText('暂无最爱游戏', POSTER_WIDTH / 2, 1090)
    setFont(ctx, 24, '400')
    ctx.fillStyle = PALETTE.muted
    ctx.fillText('本年度还没有游玩记录', POSTER_WIDTH / 2, 1136)
    ctx.restore()
    return
  }

  drawCoverThumb(ctx, cover, favorite.name.charAt(0), 104, 1000, 150, 190, 16)

  ctx.textAlign = 'left'
  setFont(ctx, 42, '600')
  ctx.fillStyle = PALETTE.text
  ctx.fillText(fitText(ctx, favorite.name, 700), 282, 1065)

  setFont(ctx, 30, '500')
  ctx.fillStyle = PALETTE.primary
  ctx.fillText(`${formatNumber(favorite.hours)} 小时`, 282, 1118)

  if (model.tags.length) {
    let cursorX = 282
    for (const tag of model.tags) {
      setFont(ctx, 22, '400')
      const text = fitText(ctx, tag, 200)
      const chipWidth = ctx.measureText(text).width + 32
      if (cursorX + chipWidth > POSTER_WIDTH - 104) break
      fillRoundRect(
        ctx,
        cursorX,
        1146,
        chipWidth,
        36,
        18,
        'rgba(99, 102, 241, 0.18)',
        'rgba(99, 102, 241, 0.45)'
      )
      ctx.textAlign = 'left'
      setFont(ctx, 22, '400')
      ctx.fillStyle = '#c7d2fe'
      ctx.fillText(text, cursorX + 16, 1171)
      cursorX += chipWidth + 12
    }
  }
  ctx.restore()
}

function drawGenres(ctx, model) {
  ctx.save()
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'
  setFont(ctx, 28, '500')
  ctx.fillStyle = PALETTE.textSecondary
  ctx.fillText('游戏类型分布', 72, 1292)

  ctx.textAlign = 'right'
  setFont(ctx, 22, '400')
  ctx.fillStyle = PALETTE.muted
  ctx.fillText('TOP 5', POSTER_WIDTH - 72, 1292)

  if (!model.genres.length) {
    ctx.textAlign = 'left'
    setFont(ctx, 28, '400')
    ctx.fillStyle = PALETTE.muted
    ctx.fillText('暂无类型分布数据', 72, 1380)
    ctx.restore()
    return
  }

  const rowHeight = 48
  const trackX = 240
  const trackWidth = 620
  const barHeight = 14

  model.genres.forEach((genre, index) => {
    const centerY = 1330 + index * rowHeight + rowHeight / 2
    ctx.textAlign = 'left'
    setFont(ctx, 26, '500')
    ctx.fillStyle = PALETTE.textSecondary
    ctx.fillText(fitText(ctx, genre.name, 150), 72, centerY + 9)

    const barY = centerY - barHeight / 2
    fillRoundRect(ctx, trackX, barY, trackWidth, barHeight, barHeight / 2, PALETTE.track)
    const fillWidth = Math.max(genre.percent > 0 ? 8 : 0, (trackWidth * genre.percent) / 100)
    if (fillWidth > 0) {
      fillRoundRect(ctx, trackX, barY, fillWidth, barHeight, barHeight / 2, genre.color)
    }

    ctx.textAlign = 'right'
    setFont(ctx, 24, '500')
    ctx.fillStyle = PALETTE.textSecondary
    ctx.fillText(formatPercent(genre.percent), POSTER_WIDTH - 72, centerY + 9)
  })
  ctx.restore()
}

function drawMonthly(ctx, model) {
  ctx.save()
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'
  setFont(ctx, 28, '500')
  ctx.fillStyle = PALETTE.textSecondary
  ctx.fillText('月度趋势', 72, 1622)

  ctx.textAlign = 'right'
  setFont(ctx, 22, '400')
  ctx.fillStyle = PALETTE.muted
  const summary = model.peak
    ? `峰值 ${model.peak.month} 月 · ${formatNumber(model.peak.hours)} 小时`
    : '暂无游玩记录'
  ctx.fillText(summary, POSTER_WIDTH - 72, 1622)

  const barWidth = 46
  const baseY = 1750
  const maxBarHeight = 96
  const gap = (POSTER_WIDTH - 144 - barWidth * 12) / 11
  const maxHours = Math.max(1, ...model.monthlyTrend.map((item) => item.hours))

  model.monthlyTrend.forEach((item, index) => {
    const x = 72 + index * (barWidth + gap)
    const barHeight = item.hours > 0 ? Math.max(4, Math.round((item.hours / maxHours) * maxBarHeight)) : 3
    if (item.hours > 0) {
      const gradient = ctx.createLinearGradient(0, baseY - barHeight, 0, baseY)
      gradient.addColorStop(0, '#818cf8')
      gradient.addColorStop(1, '#22d3ee')
      fillRoundRect(ctx, x, baseY - barHeight, barWidth, barHeight, 6, gradient)
    } else {
      fillRoundRect(ctx, x, baseY - barHeight, barWidth, barHeight, 2, 'rgba(255, 255, 255, 0.12)')
    }

    ctx.textAlign = 'center'
    setFont(ctx, 18, '400')
    ctx.fillStyle = PALETTE.muted
    ctx.fillText(String(item.month), x + barWidth / 2, baseY + 30)
  })
  ctx.restore()
}

function drawFooter(ctx) {
  drawDivider(ctx, 1806)
  drawGradientText(ctx, 'GameStats', POSTER_WIDTH / 2, 1852, 32, '700', ['#818cf8', '#22d3ee'], 'center')
  ctx.save()
  setFont(ctx, 22, '400')
  ctx.textAlign = 'center'
  ctx.textBaseline = 'alphabetic'
  ctx.fillStyle = PALETTE.muted
  ctx.fillText('记录你的游戏人生', POSTER_WIDTH / 2, 1888)
  ctx.restore()
}

function drawPoster(ctx, model, images = {}) {
  ctx.save()
  ctx.clearRect(0, 0, POSTER_WIDTH, POSTER_HEIGHT)
  drawBackground(ctx)
  drawHeader(ctx)
  drawTitle(ctx, model)
  drawDivider(ctx, 372)
  drawIdentity(ctx, model, images.avatar || null)
  drawMainStats(ctx, model)
  drawSubStats(ctx, model)
  drawFavorite(ctx, model, images.cover || null)
  drawGenres(ctx, model)
  drawMonthly(ctx, model)
  drawFooter(ctx)
  ctx.restore()
}

function dataUrlToBlob(dataUrl) {
  const parts = String(dataUrl || '').split(',')
  if (parts.length < 2) throw new Error('POSTER_EXPORT_FAILED')
  const mimeMatch = /data:([^;]+)/.exec(parts[0])
  const mime = mimeMatch ? mimeMatch[1] : 'image/png'
  const binary = typeof atob === 'function' ? atob(parts[1]) : ''
  if (!binary) throw new Error('POSTER_EXPORT_FAILED')
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return new Blob([bytes], { type: mime })
}

function canvasToBlob(canvas) {
  return new Promise((resolve, reject) => {
    let settled = false
    const done = (blob) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      if (blob && blob.size > 0) {
        resolve(blob)
      } else {
        reject(new Error('POSTER_EXPORT_FAILED'))
      }
    }
    const timer = setTimeout(() => done(null), 10000)

    if (typeof canvas.toBlob === 'function') {
      try {
        canvas.toBlob(done, 'image/png')
      } catch (error) {
        done(null)
      }
      return
    }

    try {
      done(dataUrlToBlob(canvas.toDataURL('image/png')))
    } catch (error) {
      done(null)
    }
  })
}

/**
 * 触发浏览器下载；返回是否成功发起。
 */
export function downloadBlobUrl(blobUrl, fileName) {
  if (typeof document === 'undefined' || !blobUrl) return false
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = fileName
  link.rel = 'noopener'
  link.style.display = 'none'
  document.body.appendChild(link)
  try {
    link.click()
  } finally {
    document.body.removeChild(link)
  }
  return true
}

/**
 * 导出年度报告海报。
 *
 * @param {object} payload { year, username, avatarUrl, totalHours, totalGames, achievements,
 *   completedGames, favoriteGame, genres, tags, monthlyTrend, playStyle }
 * @returns {Promise<{fileName:string, blobUrl:string, blob:Blob, width:number, height:number}>}
 * @throws Error('POSTER_EXPORT_FAILED')
 */
export async function exportYearReportPoster(payload = {}) {
  const model = createPosterModel(payload)

  await waitForFonts(1500)

  const prepared = createCanvasElement()
  if (!prepared) throw new Error('POSTER_EXPORT_FAILED')

  const images = await loadPosterImages(model)

  try {
    drawPoster(prepared.ctx, model, images)
    let blob = null
    try {
      blob = await canvasToBlob(prepared.canvas)
    } catch (error) {
      blob = null
    }

    // 图片可能因缺少 CORS 头污染画布：改为纯文字版重绘并重试一次
    if (!blob) {
      drawPoster(prepared.ctx, model, { avatar: null, cover: null })
      try {
        blob = await canvasToBlob(prepared.canvas)
      } catch (error) {
        blob = null
      }
    }

    if (!blob) throw new Error('POSTER_EXPORT_FAILED')

    const blobUrl = URL.createObjectURL(blob)
    return {
      fileName: `GameStats-年度报告-${model.year}.png`,
      blobUrl,
      blob,
      width: POSTER_WIDTH,
      height: POSTER_HEIGHT
    }
  } catch (error) {
    const failure = new Error('POSTER_EXPORT_FAILED')
    failure.cause = error
    throw failure
  }
}