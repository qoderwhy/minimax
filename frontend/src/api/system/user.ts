import request from '@/utils/request'

export interface UserItem {
  id: number
  username: string
  nickname: string
  mobile?: string
  email?: string
  status: number
  deptId?: number
  deptName?: string
  postId?: number
  postName?: string
  createTime?: string
}

export interface UserSave {
  id?: number
  username: string
  nickname: string
  password?: string
  mobile?: string
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
  mobile?: string
  status?: number
  deptId?: number
}

export function pageUser(params: UserQuery) {
  return request.page<UserItem>({ url: '/admin-api/system/user/page', params })
}

export function getUser(id: number) {
  return request.get<UserItem>({ url: `/admin-api/system/user/detail/${id}` })
}

export function saveUser(data: UserSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/user/update', data })
    : request.post<void>({ url: '/admin-api/system/user/create', data })
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
    params: { userId: id, newPassword: password }
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