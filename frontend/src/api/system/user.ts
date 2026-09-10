import request from '@/utils/request'

export interface UserItem {
  id: number
  username: string
  nickname: string
  phone?: string
  email?: string
  status: number
  deptId?: number
  deptName?: string
  postId?: number
  postName?: string
  createTime?: string
  /** 仅 detail 接口返回，用于回显已分配角色 */
  roleIds?: number[]
}

export interface UserSave {
  id?: number
  username: string
  nickname: string
  password?: string
  phone?: string
  email?: string
  status: number
  deptId?: number
  postId?: number
  roleIds: number[]
}

export interface UserQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  nickname?: string
  phone?: string
  status?: number
  deptId?: number
}

export function pageUser(params: UserQuery) {
  return request.page<UserItem>({ url: '/admin-api/system/user/page', params })
}

export function getUser(id: number) {
  return request.get<UserItem>({ url: `/admin-api/system/user/detail/${id}` })
}

export function saveUser(data: UserSave): Promise<number | boolean> {
  return data.id
    ? request.put<boolean>({ url: '/admin-api/system/user/update', data })
    : request.post<number>({ url: '/admin-api/system/user/create', data })
}

/** 分配角色（角色不在用户主表上，需单独调用；roleIds 为空表示清空角色） */
export function assignRole(userId: number, roleIds: number[]) {
  return request.put<boolean>({
    url: '/admin-api/system/user/assign-role',
    params: { userId },
    data: roleIds
  })
}

export function saveUserStatus(data: UserSave) {
  return request.put<void>({ url: '/admin-api/system/user/update', data })
}

export function deleteUser(id: number) {
  return request.delete<void>({ url: '/admin-api/system/user/delete', data: [id] })
}

export function deleteUsers(ids: number[]) {
  return request.delete<void>({ url: '/admin-api/system/user/delete', data: ids })
}

export function resetUserPassword(id: number, password: string) {
  return request.put<void>({
    url: '/admin-api/system/user/reset-password',
    data: { userId: id, newPassword: password }
  })
}

export function updateUserStatus(id: number, status: number) {
  return request.put<void>({
    url: '/admin-api/system/user/update',
    data: { id, status }
  })
}

export function exportUser(params: UserQuery) {
  return request.download<Blob>({ url: '/admin-api/system/user/export', params })
}