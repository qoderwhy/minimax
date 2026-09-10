import request from '@/utils/request'

/** 部门树节点（对应后端 DeptTreeVO，label 为部门名称） */
export interface DeptTreeItem {
  id: number
  parentId: number
  label: string
  sort?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
  children?: DeptTreeItem[]
}

/** 部门下拉项（对应后端 DeptSimpleVO，name 为部门名称） */
export interface DeptSimpleItem {
  id: number
  name: string
  parentId: number
}

export interface DeptSave {
  id?: number
  name: string
  parentId: number
  sort: number
  leader?: string
  phone?: string
  email?: string
  status: number
}

export function treeDept() {
  return request.get<DeptTreeItem[]>({ url: '/admin-api/system/dept/tree' })
}

export function listDept() {
  return request.get<DeptSimpleItem[]>({ url: '/admin-api/system/dept/simple-list' })
}

export function saveDept(data: DeptSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dept/update', data })
    : request.post<void>({ url: '/admin-api/system/dept/create', data })
}

export function deleteDept(id: number) {
  return request.delete<void>({ url: '/admin-api/system/dept/delete', data: [id] })
}