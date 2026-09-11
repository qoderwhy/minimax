import request from '@/utils/request'

export interface ConfigItem {
  id: number
  configName: string
  configKey: string
  configValue: string
  configType: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ConfigSave {
  id?: number
  configName: string
  configKey: string
  configValue: string
  configType: string
  remark?: string
}

export function pageConfig(params: {
  pageNum?: number
  pageSize?: number
  configKey?: string
  configName?: string
}) {
  return request.page<ConfigItem>({ url: '/system/config/page', params })
}

export function listConfig() {
  return request.get<ConfigItem[]>({ url: '/system/config/list' })
}

export function saveConfig(data: ConfigSave) {
  return data.id
    ? request.put<void>({ url: '/system/config/update', data })
    : request.post<void>({ url: '/system/config/create', data })
}

export function deleteConfig(id: number) {
  return request.delete<void>({ url: '/system/config/delete', data: [id] })
}