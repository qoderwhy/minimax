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

  function reset() {
    buttons.value = []
    routes.value = []
    routesLoaded.value = false
  }

  return { buttons, routes, routesLoaded, setRoutes, loadPerms, fetchRoutes, reset }
})
