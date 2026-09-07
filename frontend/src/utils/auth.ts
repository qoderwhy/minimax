/**
 * Token 存取工具。
 * 后端 Sa-Token 通过 satoken 请求头校验，前端统一在 request 拦截器注入。
 * 使用 localStorage 存储，页面刷新不丢失登录态。
 */
const TOKEN_KEY = 'qkit_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
