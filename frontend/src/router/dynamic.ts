import type { RouteVO } from '@/types/api'

const modules = import.meta.glob('@/views/**/*.vue')

function loadComponent(component: string) {
  if (component === 'Layout') {
    return () => import('@/layout/index.vue')
  }
  // 简化路径：system/user/index → /src/views/system/user/index.vue
  const path = `/src/views/${component}.vue`
  return modules[path]
}

export function transformRoutes(routes: RouteVO[]) {
  return routes.map((r) => {
    const route: any = {
      path: r.path,
      name: r.name,
      meta: {
        title: r.meta?.title,
        icon: r.meta?.icon,
        hidden: r.meta?.hidden,
        keepAlive: r.meta?.keepAlive
      }
    }
    if (r.component) {
      const loader = loadComponent(r.component)
      route.component = loader || (() => import('@/views/error/404.vue'))
    }
    if (r.redirect) route.redirect = r.redirect
    if (r.children && r.children.length > 0) {
      route.children = transformRoutes(r.children)
    }
    return route
  })
}
