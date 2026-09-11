import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { R } from '@/types/api'
import { useUserStore } from '@/stores/user'

/** 业务成功码（与后端 R.code 对齐） */
const SUCCESS_CODE = 200

const request: AxiosInstance = axios.create({
  baseURL: (import.meta.env as any).VITE_API_BASE_URL || '/',
  timeout: 30000
})

/** 标识当前是否已弹出重新登录提示框，避免并发触发多个弹窗 */
let isReloginShown = false

// 请求拦截器：注入 Sa-Token 凭证
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['satoken'] = userStore.token
  }
  return config
})

// 响应拦截器：统一识别业务码与 HTTP 状态，未登录（401）统一弹窗后跳转
request.interceptors.response.use(
  (resp) => {
    const data = resp.data as R
    // 文件流等非 Result 结构直接原样返回
    if (data === null || typeof data !== 'object' || !('code' in data)) {
      return resp
    }
    if (data.code === SUCCESS_CODE) {
      return resp
    }
    if (data.code === 401) {
      handleUnauthorized()
      return Promise.reject(data)
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(err?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

/** 未登录统一处理：清除本地凭证并确认后跳转登录页 */
function handleUnauthorized() {
  if (isReloginShown) return
  isReloginShown = true
  useUserStore().logout(true)
  ElMessageBox.confirm('登录状态已过期，是否重新登录？', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      const redirect = encodeURIComponent(location.hash.slice(1) || '/')
      location.href = `/#/login?redirect=${redirect}`
    })
    .catch(() => {})
    .finally(() => {
      isReloginShown = false
    })
}

/** 统一请求基类：成功时返回已解包的 res.data */
async function requestData<T>(config: AxiosRequestConfig): Promise<T> {
  const resp = await request(config)
  return resp.data.data as T
}

interface QueryParams {
  params?: object
}

/** 便捷方法：get/post/put/delete 返回已解包的 res.data */
export const http = {
  get<T = any>(url: string, params?: object): Promise<T> {
    return requestData<T>({ url, method: 'get', params })
  },
  post<T = any>(url: string, data?: object, options?: QueryParams): Promise<T> {
    return requestData<T>({ url, method: 'post', data, ...options })
  },
  put<T = any>(url: string, data?: object, options?: QueryParams): Promise<T> {
    return requestData<T>({ url, method: 'put', data, ...options })
  },
  delete<T = any>(url: string, params?: object): Promise<T> {
    return requestData<T>({ url, method: 'delete', params })
  }
}

/** 分页请求：返回 { list, total }，从 R.data（列表）与 R.total（总数）组装 */
async function requestPage<T>(config: AxiosRequestConfig): Promise<{ list: T[]; total: number }> {
  const resp = await request(config)
  const r = resp.data as R
  return { list: (r.data as T[]) ?? [], total: r.total ?? 0 }
}

/**
 * 文件下载请求：用于导出等场景，返回完整 axios response（含 .data 的 blob）。
 * 通过 responseType: 'blob' 让响应拦截器将文件流原样返回。
 */
function requestDownload<T = Blob>(config: AxiosRequestConfig): Promise<AxiosResponse<T>> {
  return request({ ...config, responseType: 'blob' }) as Promise<AxiosResponse<T>>
}

export default {
  get<T = any>(config: AxiosRequestConfig): Promise<T> {
    return requestData<T>({ ...config, method: 'get' })
  },
  page<T = any>(config: AxiosRequestConfig): Promise<{ list: T[]; total: number }> {
    return requestPage<T>({ ...config, method: 'get' })
  },
  download<T = Blob>(config: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return requestDownload<T>({ ...config, method: 'get' })
  },
  post<T = any>(config: AxiosRequestConfig): Promise<T> {
    return requestData<T>({ ...config, method: 'post' })
  },
  put<T = any>(config: AxiosRequestConfig): Promise<T> {
    return requestData<T>({ ...config, method: 'put' })
  },
  delete<T = any>(config: AxiosRequestConfig): Promise<T> {
    return requestData<T>({ ...config, method: 'delete' })
  }
}