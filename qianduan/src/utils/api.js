import axios from 'axios'
import { useUserStore } from '@/store/user'
import router from '@/router'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    // 上传 FormData 时必须删除默认 Content-Type（application/json），
    // 否则 XHR 不会自动生成 multipart boundary → 后端收到 application/json → 500
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 401:
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
          break
        case 403:
          // 优先展示后端文案（隐私保护等业务提示），缺失时才兜底
          ElMessage.error(response.data?.message || '没有权限访问')
          break
        case 404:
          // 优先展示后端文案（通知不存在/分享记录不存在等），缺失时才兜底
          ElMessage.error(response.data?.message || '请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default api