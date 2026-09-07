import request from '@/utils/request'

export interface UserItem {
  id: string
  username: string
  nickname: string
  mobile?: string
  email?: string
  status: number
  deptId?: string
  deptName?: string
  createTime?: string
}

export interface UserSave {
  id?: string
  username: string
  nickname: string
  password?: string
  mobile?: string
  email?: string
  status: number
  deptId?: string
  roleIds: string[]
  postIds?: string[]
}

export interface UserQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  nickname?: string
  mobile?: string
  status?: number
  deptId?: string
}

export function pageUser(params: UserQuery) {
  return request.page<UserItem>({ url: '/admin-api/system/user/page', params })
}

export function getUser(id: string) {
  return request.get<UserItem>({ url: `/admin-api/system/user/detail/${id}` })
}

export function saveUser(data: UserSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/user/update', data })
    : request.post<void>({ url: '/admin-api/system/user/create', data })
}

export function deleteUser(id: string) {
  return request.delete<void>({ url: '/admin-api/system/user/delete', data: [id] })
}

export function resetUserPassword(id: string, password: string) {
  return request.put<void>({
    url: `/admin-api/system/user/reset-password?userId=${id}&newPassword=${password}`
  })
}

export function updateUserStatus(id: string, status: number) {
  return request.put<void>({
    url: `/admin-api/system/user/update?status=${status}`,
    data: { id, status }
  })
}
