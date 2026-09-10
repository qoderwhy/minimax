import request from '@/utils/request'

export interface OperLogItem {
  id: number
  module: string
  name: string
  userId?: number
  username?: string
  ip?: string
  userAgent?: string
  method?: string
  requestUrl?: string
  requestMethod?: string
  requestParam?: string
  responseResult?: string
  status: number
  errorMsg?: string
  costMs?: number
  operTime?: string
}

export function pageOperLog(params: {
  pageNum?: number
  pageSize?: number
  module?: string
  username?: string
  status?: number
  /** 与后端 OperLogQueryDTO 字段名对齐 */
  beginTime?: string
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
  return request.delete<void>({ url: '/admin-api/system/oper-log/clean' })
}