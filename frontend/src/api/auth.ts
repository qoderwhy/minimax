import request from '@/utils/request'
import type { LoginVO, LoginUserVO, CaptchaVO } from '@/types/api'

export function getCaptcha() {
  return request.get<CaptchaVO>({ url: '/admin-api/auth/captcha' })
}

export function login(data: { username: string; password: string; captchaId?: string; captchaCode?: string }) {
  return request.post<LoginVO>({ url: '/admin-api/auth/login', data })
}

export function logout() {
  return request.post<void>({ url: '/admin-api/auth/logout' })
}

export function getMe() {
  return request.get<LoginUserVO>({ url: '/admin-api/auth/me' })
}

export function getRoute() {
  return request.get<any[]>({ url: '/admin-api/system/menu/route' })
}

export function getPerms() {
  return request.get<string[]>({ url: '/admin-api/auth/perms' })
}
