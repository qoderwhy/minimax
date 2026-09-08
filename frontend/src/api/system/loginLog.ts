import request from '@/utils/request'

export interface LoginLogItem {
  id: number
  username?: string
  ip?: string
  status: number
  msg?: string
  os?: string
  browser?: string
  createTime?: string
}

export function pageLoginLog(params: {
  pageNum?: number
  pageSize?: number
  username?: string
  status?: number
  startTime?: string
  endTime?: string
}) {
  return request.page<LoginLogItem>({
    url: '/admin-api/system/login-log/page',
    params
  })
}

export function deleteLoginLog(id: number) {
  return request.delete<void>({ url: '/admin-api/system/login-log/delete', data: [id] })
}

export function cleanLoginLog() {
  return request.delete<void>({ url: '/admin-api/system/login-log/delete', data: [] })
}