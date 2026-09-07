import request from '@/utils/request'

export interface DictItem {
  id: string
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
  id: string
  type: string
  name: string
  status: number
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

export function saveDictType(data: DictType) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dict/update', data })
    : request.post<void>({ url: '/admin-api/system/dict/create', data })
}

export function deleteDictType(id: string) {
  return request.delete<void>({ url: '/admin-api/system/dict/delete', data: [id] })
}

export async function pageDictItem(params: { pageNum?: number; pageSize?: number; type?: string; label?: string }) {
  const items = await request.get<DictItem[]>({ url: '/admin-api/system/dict/items', params })
  return { list: items, total: items.length }
}

export function saveDictItem(data: DictItem) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dict/item/update', data })
    : request.post<void>({ url: '/admin-api/system/dict/item/create', data })
}

export function deleteDictItem(id: string) {
  return request.delete<void>({ url: '/admin-api/system/dict/item/delete', data: [id] })
}

export function listDictItem(dictType: string) {
  return request.get<DictItem[]>({ url: `/admin-api/system/dict/items?type=${dictType}` })
}
