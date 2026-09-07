import request from '@/utils/request'

export interface DeptItem {
  id: string
  parentId: string
  label: string
  sort?: number
  leader?: string
  mobile?: string
  email?: string
  status?: number
  children?: DeptItem[]
}

export interface DeptSave {
  id?: string
  name: string
  parentId: string
  sort: number
  leader?: string
  mobile?: string
  email?: string
  status: number
}

export function treeDept() {
  return request.get<DeptItem[]>({ url: '/admin-api/system/dept/tree' })
}

export function listDept() {
  return request.get<DeptItem[]>({ url: '/admin-api/system/dept/simple-list' })
}

export function saveDept(data: DeptSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dept/update', data })
    : request.post<void>({ url: '/admin-api/system/dept/create', data })
}

export function deleteDept(id: string) {
  return request.delete<void>({ url: '/admin-api/system/dept/delete', data: [id] })
}
