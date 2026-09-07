import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getRoute, getPerms } from '@/api/auth'

export const usePermissionStore = defineStore('permission', () => {
  const buttons = ref<string[]>([])
  const routes = ref<RouteRecordRaw[]>([])
  const routesLoaded = ref(false)

  function setRoutes(r: RouteRecordRaw[]) {
    routes.value = r
  }

  async function loadPerms() {
    if (routesLoaded.value) return
    buttons.value = await getPerms()
    routesLoaded.value = true
  }

  async function fetchRoutes() {
    return await getRoute()
  }

  /** 超级管理员通配权限 */
  const ALL_PERMISSION = '*:*:*'

  /** 判断是否拥有某权限点（支持超级管理员通配 *:*:*），数组语义为全部满足 */
  function hasPermission(perm: string | string[]): boolean {
    if (buttons.value.includes(ALL_PERMISSION)) return true
    const perms = Array.isArray(perm) ? perm : [perm]
    return perms.every((p) => buttons.value.includes(p))
  }

  function reset() {
    buttons.value = []
    routes.value = []
    routesLoaded.value = false
  }

  return {
    buttons,
    routes,
    routesLoaded,
    setRoutes,
    loadPerms,
    fetchRoutes,
    hasPermission,
    reset
  }
})
