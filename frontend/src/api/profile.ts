import request from '@/utils/request'

/** 个人资料（对齐后端 UserVO） */
export interface ProfileVO {
  id: number
  username: string
  nickname: string
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  sex?: number
  sexLabel?: string
  deptId?: number
  deptName?: string
  postId?: number
  postName?: string
  status?: number
  statusLabel?: string
  loginIp?: string
  loginDate?: string
  createTime?: string
  remark?: string
  roleIds?: number[]
}

/** 更新个人资料请求体 */
export interface ProfileUpdate {
  nickname?: string
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  sex?: number
  remark?: string
}

/** 修改密码请求体 */
export interface ChangePassword {
  oldPassword: string
  newPassword: string
}

/** 获取个人资料 */
export function getProfile() {
  return request.get<ProfileVO>({ url: '/admin-api/system/user/profile' })
}

/** 更新个人资料 */
export function updateProfile(data: ProfileUpdate) {
  return request.put<void>({ url: '/admin-api/system/user/profile', data })
}

/** 修改密码（本人） */
export function changePassword(data: ChangePassword) {
  return request.put<void>({ url: '/admin-api/system/user/profile/password', data })
}