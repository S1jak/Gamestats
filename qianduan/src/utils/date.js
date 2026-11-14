/**
 * 日期与数值格式化工具（消费偏好 dateFormat / showDecimal）
 *
 * 兼容后端 Jackson 对 LocalDate 的两种序列化形态：
 *   1) 字符串：'2026-07-02' / '2026-07-02 21:00:00' / ISO
 *   2) 数组：  [2026, 7, 2]
 */

function pad(value) {
  return String(value).padStart(2, '0')
}

function toDateParts(value) {
  if (value === null || value === undefined || value === '') return null

  if (Array.isArray(value)) {
    const [y, m, d] = value
    if (!y || !m || !d) return null
    return { y: Number(y), m: Number(m), d: Number(d) }
  }

  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) return null
    return { y: value.getFullYear(), m: value.getMonth() + 1, d: value.getDate() }
  }

  if (typeof value === 'string') {
    const match = value.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
    if (match) {
      return { y: Number(match[1]), m: Number(match[2]), d: Number(match[3]) }
    }
    return null
  }

  if (typeof value === 'number') {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return null
    return { y: date.getFullYear(), m: date.getMonth() + 1, d: date.getDate() }
  }

  return null
}

/**
 * 按偏好格式输出日期；无法解析时返回空串（不渲染 undefined/NaN）
 * @param {*} value
 * @param {'YYYY-MM-DD'|'MM/DD/YYYY'} dateFormat
 * @returns {string}
 */
export function formatDate(value, dateFormat = 'YYYY-MM-DD') {
  const parts = toDateParts(value)
  if (!parts) return ''
  const { y, m, d } = parts
  if (dateFormat === 'MM/DD/YYYY') {
    return `${pad(m)}/${pad(d)}/${y}`
  }
  return `${y}-${pad(m)}-${pad(d)}`
}

/**
 * 按 showDecimal 输出 1 位小数或整数
 * @param {*} value
 * @param {boolean} showDecimal
 * @returns {string}
 */
export function formatDecimal(value, showDecimal = true) {
  const num = Number(value)
  if (!Number.isFinite(num)) return showDecimal ? '0.0' : '0'
  return showDecimal ? num.toFixed(1) : String(Math.round(num))
}