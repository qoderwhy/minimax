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
  /** 自定义数据权限的部门；菜单授权走 assignMenu，不在此提交 */
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

export function saveRole(data: RoleSave): Promise<number | boolean> {
  return data.id
    ? request.put<boolean>({ url: '/admin-api/system/role/update', data })
    : request.post<number>({ url: '/admin-api/system/role/create', data })
}

export function deleteRole(id: number) {
  return request.delete<void>({ url: '/admin-api/system/role/delete', data: [id] })
}

export function getRoleDeptIds(roleId: number) {
  return request.get<number[]>({ url: '/admin-api/system/role/dept-ids', params: { roleId } })
}

/** 查询角色已分配菜单 ID（与 assign-dept 对称，供授权弹窗回显） */
export function getRoleMenuIds(roleId: number) {
  return request.get<number[]>({ url: '/admin-api/system/role/menu-ids', params: { roleId } })
}

/** 分配菜单（menuIds 为空表示清空该角色的菜单权限） */
export function assignMenu(roleId: number, menuIds: number[]) {
  return request.put<boolean>({ url: '/admin-api/system/role/assign-menu', params: { roleId }, data: menuIds })
}