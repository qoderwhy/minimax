import request from '@/utils/request'

export interface RoleItem {
  id: string
  name: string
  code: string
  status: number
  dataScope: number
  sort: number
  remark?: string
  createTime?: string
}

export interface RoleSave {
  id?: string
  name: string
  code: string
  status: number
  dataScope: number
  sort: number
  remark?: string
  menuIds: string[]
  deptIds?: string[]
}

export interface RoleQuery {
  pageNum?: number
  pageSize?: number
  name?: string
  code?: string
  status?: number
}

export function pageRole(params: RoleQuery) {
  return request.page<RoleItem>({ url: '/admin-api/system/role/page', params })
}

export function listRole() {
  return request.get<RoleItem[]>({ url: '/admin-api/system/role/list' })
}

export function getRole(id: string) {
  return request.get<RoleItem>({ url: `/admin-api/system/role/detail/${id}` })
}

export function saveRole(data: RoleSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/role/update', data })
    : request.post<void>({ url: '/admin-api/system/role/create', data })
}

export function deleteRole(id: string) {
  return request.delete<void>({ url: '/admin-api/system/role/delete', data: [id] })
}
