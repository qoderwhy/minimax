import request from '@/utils/request'

export interface DictItem {
  id: number
  dictType: string
  label: string
  value: string
  sort: number
  status: number
  cssClass?: string
  remark?: string
  defaultFlag?: number
}

export interface DictType {
  id: number
  type: string
  name: string
  status: number
  remark?: string
}

export interface DictTypeSave {
  id?: number
  type: string
  name: string
  status: number
  remark?: string
}

export interface DictItemSave {
  id?: number
  dictType: string
  label: string
  value: string
  sort: number
  status: number
  cssClass?: string
  remark?: string
}

export function pageDictType(params: {
  pageNum?: number
  pageSize?: number
  type?: string
  name?: string
  status?: number
}) {
  return request.page<DictType>({ url: '/admin-api/system/dict/page', params })
}

export function saveDictType(data: DictTypeSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dict/update', data })
    : request.post<void>({ url: '/admin-api/system/dict/create', data })
}

export function deleteDictType(id: number) {
  return request.delete<void>({ url: '/admin-api/system/dict/delete', data: [id] })
}

/**
 * 字典项分页查询（管理端）：走后端分页，避免把某类型下的全部字典项拉到前端再切片。
 * 入参沿用页面上的 type 命名，请求时映射为后端的 dictType。
 */
export function pageDictItem(params: {
  pageNum?: number
  pageSize?: number
  type?: string
  label?: string
  status?: number
}) {
  return request.page<DictItem>({
    url: '/admin-api/system/dict/item/page',
    params: {
      dictType: params.type,
      label: params.label,
      status: params.status,
      pageNum: params.pageNum,
      pageSize: params.pageSize
    }
  })
}

export function saveDictItem(data: DictItemSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dict/item/update', data })
    : request.post<void>({ url: '/admin-api/system/dict/item/create', data })
}

export function deleteDictItem(id: number) {
  return request.delete<void>({ url: '/admin-api/system/dict/item/delete', data: [id] })
}

/**
 * 字典下拉数据源。
 * 走后端公开接口 /dict/type/{typeCode}（仅返回启用项），
 * 避免普通用户没有 system:dict:list 权限时 403。
 */
export function listDictItem(dictType: string) {
  return request.get<DictItem[]>({ url: `/admin-api/system/dict/type/${encodeURIComponent(dictType)}` })
}