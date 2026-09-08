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

/** 字典项列表：后端 /items 返回指定类型全部字典项（非分页），前端由调用方自行分页 */
export function pageDictItem(params: {
  pageNum?: number
  pageSize?: number
  type?: string
  label?: string
}) {
  const { type } = params
  return request.get<DictItem[]>({ url: '/admin-api/system/dict/items', params: { type } }).then(
    (list) => {
      const filtered = labelFilter(list, params.label)
      const pageNum = params.pageNum || 1
      const pageSize = params.pageSize || 10
      const start = (pageNum - 1) * pageSize
      return { list: filtered.slice(start, start + pageSize), total: filtered.length }
    }
  )
}

function labelFilter(list: DictItem[], label?: string) {
  if (!label) return list
  return list.filter((i) => i.label.includes(label))
}

export function saveDictItem(data: DictItemSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dict/item/update', data })
    : request.post<void>({ url: '/admin-api/system/dict/item/create', data })
}

export function deleteDictItem(id: number) {
  return request.delete<void>({ url: '/admin-api/system/dict/item/delete', data: [id] })
}

export function listDictItem(dictType: string) {
  return request.get<DictItem[]>({ url: '/admin-api/system/dict/items', params: { type: dictType } })
}