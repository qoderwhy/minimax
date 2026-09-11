import request from '@/utils/request'
import type { LoginVO, LoginUserVO, CaptchaVO } from '@/types/api'

export function getCaptcha() {
  return request.get<CaptchaVO>({ url: '/auth/captcha' })
}

export function login(data: { username: string; password: string; captchaId?: string; captchaCode?: string }) {
  return request.post<LoginVO>({ url: '/auth/login', data })
}

export function logout() {
  return request.post<void>({ url: '/auth/logout' })
}

export function getMe() {
  return request.get<LoginUserVO>({ url: '/auth/me' })
}

export function getRoute() {
  return request.get<any[]>({ url: '/system/menu/route' })
}

export function getPerms() {
  return request.get<string[]>({ url: '/auth/perms' })
}
