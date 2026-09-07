/** 统一响应结构 */
export interface R<T = any> {
  code: number
  message: string
  data: T
  total?: number
  pageNum?: number
  pageSize?: number
  traceId?: string
}

/** 登录用户信息 */
export interface LoginUserVO {
  userId: string
  username: string
  nickname: string
  realName: string
  avatar: string
  deptId: string
  deptName?: string
  roles: string[]
  permissions: string[]
}

/** 登录响应 */
export interface LoginVO {
  token: string
  userId: string
  username: string
  nickname: string
}

/** 验证码 */
export interface CaptchaVO {
  captchaId: string
  captchaImage: string
}

/** 字典项 */
export interface DictItemVO {
  id: string
  dictType: string
  label: string
  value: string
  sort: number
  status: number
  cssClass: string
}

/** 路由 */
export interface RouteVO {
  id: string
  name: string
  path: string
  component: string
  redirect?: string
  meta: RouteMetaVO
  children: RouteVO[]
}

export interface RouteMetaVO {
  title: string
  icon: string
  hidden: boolean
  keepAlive: boolean
  perm?: string
}
