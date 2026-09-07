import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginUserVO, LoginVO } from '@/types/api'
import { login as apiLogin, logout as apiLogout, getMe } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { usePermissionStore } from '@/stores/permission'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const userInfo = ref<LoginUserVO | null>(null)

  function setLoginVO(data: LoginVO) {
    token.value = data.token
    setToken(data.token)
  }

  async function login(dto: { username: string; password: string; captchaId?: string; captchaCode?: string }) {
    const res = await apiLogin(dto)
    setLoginVO(res)
    return res
  }

  async function fetchUserInfo() {
    userInfo.value = await getMe()
    return userInfo.value
  }

  async function logout(skipRequest = false) {
    if (!skipRequest) {
      try {
        await apiLogout()
      } catch (e) {
        // ignore
      }
    }
    token.value = ''
    userInfo.value = null
    removeToken()
    usePermissionStore().reset()
  }

  return { token, userInfo, login, logout, fetchUserInfo, setLoginVO }
})
