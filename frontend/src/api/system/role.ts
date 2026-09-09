import request from '@/utils/request'

export interface RoleItem {
  id: number
  name: string
  code: string
  status: number
  dataScope: number
  sort: number
  remark?: string
  createTime?: string
}

export interface RoleSave {
  id?: number
  name: string
  code: string
  status: number
  dataScope: number
  sort: number
  remark?: string
  menuIds: number[]
  deptIds?: number[]
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

export function getRole(id: number) {
  return request.get<RoleItem>({ url: `/admin-api/system/role/detail/${id}` })
}

export function saveRole(data: RoleSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/role/update', data })
    : request.post<void>({ url: '/admin-api/system/role/create', data })
}

export function deleteRole(id: number) {
  return request.delete<void>({ url: '/admin-api/system/role/delete', data: [id] })
}

export function getRoleDeptIds(roleId: number) {
  return request.get<number[]>({ url: '/admin-api/system/role/dept-ids', params: { roleId } })
}

export function assignRoleDept(roleId: number, deptIds: number[]) {
  return request.put<void>({ url: '/admin-api/system/role/assign-dept', params: { roleId }, data: deptIds })
}