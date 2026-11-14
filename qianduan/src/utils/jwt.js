/**
 * JWT payload 解析（仅体验层）
 *
 * 说明：
 * 1. 只解码 payload，不校验签名；服务端 401 仍是最终防线。
 * 2. payload 的 username 可能是中文，必须走 TextDecoder('utf-8')，
 *    直接用 atob 拼字符串会得到乱码。
 * 3. 任何异常（格式非法、base64 非法、JSON 非法、缺 exp）一律返回 null/false，不抛出。
 */

const BASE64_URL_CHARS = /-_/g

/**
 * base64url 字符串 → utf-8 文本
 * @param {string} segment
 * @returns {string|null}
 */
function decodeBase64Url(segment) {
  if (typeof segment !== 'string' || !segment) return null
  const normalized = segment.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)
  const binary = atob(padded)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new TextDecoder('utf-8').decode(bytes)
}

/**
 * 解码 JWT payload
 * @param {string} token
 * @returns {{exp: number|null, userId: any, username: any}|null}
 */
export function decodePayload(token) {
  if (typeof token !== 'string' || !token) return null
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const json = decodeBase64Url(parts[1])
    if (!json) return null
    const payload = JSON.parse(json)
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return null
    const exp = Number(payload.exp)
    return {
      exp: Number.isFinite(exp) ? exp : null,
      userId: payload.userId ?? null,
      username: payload.username ?? null
    }
  } catch (e) {
    return null
  }
}

/**
 * 取 token 过期时间（毫秒时间戳）；无法解析或无 exp 返回 null
 * @param {string} token
 * @returns {number|null}
 */
export function getExpiresAt(token) {
  const payload = decodePayload(token)
  if (!payload || payload.exp === null) return null
  return payload.exp * 1000
}

/**
 * token 是否仍有效（默认预留 30s 偏差，避免临界时刻仍放行）
 * @param {string} token
 * @param {number} skewMs
 * @returns {boolean}
 */
export function isTokenValid(token, skewMs = 30000) {
  const expiresAt = getExpiresAt(token)
  if (expiresAt === null) return false
  return expiresAt - Math.max(0, Number(skewMs) || 0) > Date.now()
}

export const JWT_BASE64_URL_CHARS = BASE64_URL_CHARS