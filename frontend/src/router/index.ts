import { createRouter, createWebHashHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { staticRoutes } from './routes'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { transformRoutes } from './dynamic'

NProgress.configure({ showSpinner: false })

const router = createRouter({
  history: createWebHashHistory(),
  routes: staticRoutes
})

const WHITE_LIST = ['/login', '/404']

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  if (WHITE_LIST.includes(to.path)) {
    if (to.path === '/login' && userStore.token) {
      next('/')
      NProgress.done()
      return
    }
    next()
    return
  }
  if (!userStore.token) {
    next(`/login?redirect=${to.fullPath}`)
    NProgress.done()
    return
  }
  const permStore = usePermissionStore()
  if (!permStore.routesLoaded) {
    try {
      const routes = await permStore.fetchRoutes()
      const dynamicRoutes = transformRoutes(routes as any)
      dynamicRoutes.forEach((r) => router.addRoute(r))
      permStore.setRoutes(dynamicRoutes)
      permStore.loadPerms()
      next({ ...to, replace: true })
    } catch (e) {
      userStore.logout()
      next('/login')
      NProgress.done()
    }
    return
  }
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
