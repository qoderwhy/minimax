import request from '@/utils/request'

export interface LoginLogItem {
  id: number
  username?: string
  ip?: string
  status: number
  message?: string
  os?: string
  browser?: string
  loginTime?: string
}

export function pageLoginLog(params: {
  pageNum?: number
  pageSize?: number
  username?: string
  status?: number
  /** 与后端 LoginLogQueryDTO 字段名对齐 */
  beginTime?: string
  endTime?: string
}) {
  return request.page<LoginLogItem>({
    url: '/system/login-log/page',
    params
  })
}

export function deleteLoginLog(id: number) {
  return request.delete<void>({ url: '/system/login-log/delete', data: [id] })
}

export function cleanLoginLog() {
  return request.delete<void>({ url: '/system/login-log/clean' })
}