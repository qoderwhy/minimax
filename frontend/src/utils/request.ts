import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { R } from '@/types/api'
import { useUserStore } from '@/stores/user'

const request: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 30000
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['satoken'] = userStore.token
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const data = resp.data as R
    if (data.code === 200) {
      return resp
    }
    if (data.code === 401) {
      const userStore = useUserStore()
      userStore.logout(true)
      window.location.href = '/#/login'
      return Promise.reject(data)
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (err) => {
    ElMessage.error(err.message || '网络异常')
    return Promise.reject(err)
  }
)

/** 统一返回 R.data */
async function unwrap<T>(promise: Promise<any>): Promise<T> {
  const resp = await promise
  return resp.data.data as T
}

/** 分页接口：返回 R.data（列表）与 R.total（总数） */
async function unwrapPage<T>(promise: Promise<any>): Promise<{ list: T[]; total: number }> {
  const resp = await promise
  const r = resp.data as R
  return { list: (r.data as T[]) ?? [], total: r.total ?? 0 }
}

export default {
  get<T = any>(config: AxiosRequestConfig): Promise<T> {
    return unwrap(request.get<R>(config.url!, { params: config.params }))
  },
  page<T = any>(config: AxiosRequestConfig): Promise<{ list: T[]; total: number }> {
    return unwrapPage(request.get<R>(config.url!, { params: config.params }))
  },
  post<T = any>(config: AxiosRequestConfig): Promise<T> {
    return unwrap(request.post<R>(config.url!, config.data))
  },
  put<T = any>(config: AxiosRequestConfig): Promise<T> {
    return unwrap(request.put<R>(config.url!, config.data))
  },
  del<T = any>(config: AxiosRequestConfig): Promise<T> {
    return unwrap(request.delete<R>(config.url!, { data: config.data }))
  },
  delete<T = any>(config: AxiosRequestConfig): Promise<T> {
    return unwrap(request.delete<R>(config.url!, { data: config.data }))
  }
}
