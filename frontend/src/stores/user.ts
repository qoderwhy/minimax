import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginUserVO, LoginVO } from '@/types/api'
import { login as apiLogin, logout as apiLogout, getMe } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { usePermissionStore } from '@/stores/permission'
import { useAppStore } from '@/stores/app'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const userInfo = ref<LoginUserVO | null>(null)

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 当前用户权限点集合（透传自登录信息，供按钮级权限判断） */
  const permissions = computed(() => userInfo.value?.permissions ?? [])

  function setLoginVO(data: LoginVO) {
    token.value = data.token
    setToken(data.token)
  }

  async function login(dto: {
    username: string
    password: string
    captchaId?: string
    captchaCode?: string
  }) {
    const res = await apiLogin(dto)
    setLoginVO(res)
    return res
  }

  /** 拉取当前用户资料，并将权限点同步到权限 store */
  async function fetchUserInfo() {
    userInfo.value = await getMe()
    usePermissionStore().buttons = userInfo.value?.permissions ?? []
    return userInfo.value
  }

  async function logout(skipRequest = false) {
    if (!skipRequest) {
      try {
        await apiLogout()
      } catch (e) {
        // 忽略登出接口异常，保证本地状态清理
      }
    }
    token.value = ''
    userInfo.value = null
    removeToken()
    const permStore = usePermissionStore()
    // 移除已注册的动态路由，避免切换账号后路由残留
    const { default: router } = await import('@/router')
    permStore.routes.forEach((r) => {
      if (r.name) router.removeRoute(String(r.name))
    })
    permStore.reset()
    useAppStore().resetTags()
  }

  return { token, userInfo, permissions, isLoggedIn, login, logout, fetchUserInfo, setLoginVO }
})