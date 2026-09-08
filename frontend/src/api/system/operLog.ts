import request from '@/utils/request'

export interface OperLogItem {
  id: number
  module: string
  name: string
  userId?: number
  username?: string
  ip?: string
  status: number
  costMs?: number
  createTime?: string
  method?: string
  url?: string
  reqParam?: string
  resp?: string
}

export function pageOperLog(params: {
  pageNum?: number
  pageSize?: number
  module?: string
  username?: string
  status?: number
  startTime?: string
  endTime?: string
}) {
  return request.page<OperLogItem>({
    url: '/admin-api/system/oper-log/page',
    params
  })
}

export function deleteOperLog(id: number) {
  return request.delete<void>({ url: '/admin-api/system/oper-log/delete', data: [id] })
}

export function cleanOperLog() {
  return request.delete<void>({ url: '/admin-api/system/oper-log/delete', data: [] })
}