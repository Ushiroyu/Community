import axios from 'axios'
import { useUserStore } from '@/store/user'

// API 基地址：优先显式网关，其次自定义前缀，最后默认指向后端网关（便于部署测试）
const baseURL =
  import.meta.env.VITE_GATEWAY ||
  import.meta.env.VITE_API_PREFIX ||
  (import.meta.env.DEV ? '/api' : 'http://8.148.158.99:8080')

const http = axios.create({
  baseURL,
  timeout: 15000,
  withCredentials: false
})

http.interceptors.request.use((config) => {
  try {
    const store = useUserStore()
    if (store?.token) config.headers['Authorization'] = 'Bearer ' + store.token
  } catch {}
  return config
})

http.interceptors.response.use(
  (res) => {
    const data = res?.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (Number(data.code) !== 0) {
        const msg = data.msg || data.message || '请求失败'
        const err = new Error(msg)
        err.__response = data
        return Promise.reject(err)
      }
      return data
    }
    return data
  },
  (err) => {
    let msg = err?.message || '请求失败'
    try {
      const status = err?.response?.status
      if (status === 401 || status === 403) {
        try {
          // 清理本地会话并跳转登录
          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('role')
          localStorage.removeItem('username')
        } catch {}
        if (typeof window !== 'undefined') {
          const redirect = encodeURIComponent(window.location.pathname + window.location.search)
          window.location.href = `/login?redirect=${redirect}`
        }
      }
      const d = err?.response?.data
      if (typeof d === 'string') msg = d
      else if (d?.message) msg = d.message
      else if (d?.msg) msg = d.msg
      else if (d?.error) msg = d.error
      else if (d) msg = JSON.stringify(d)
    } catch {}
    return Promise.reject(new Error(msg))
  }
)

export default http
