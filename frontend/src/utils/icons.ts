import type { App } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

/**
 * 注册 Element Plus 图标组件。
 *
 * 这里保持全量注册而非白名单，原因：
 * 1. 菜单图标由后端菜单表（sys_menu.icon）配置，侧边栏通过
 *    `<component :is="menu.meta.icon" />` 以字符串动态渲染，集合不可穷举；
 *    白名单会导致管理员配了未登记的图标时图标静默消失；
 * 2. 菜单管理页的图标选择器（components/IconSelect.vue）本身就需要列出全部图标，
 *    因此图标库整体必然进入产物，白名单只能改变"放主包还是放独立 chunk"；
 * 3. 图标库已由 vite.config.ts 拆为独立 chunk，不参与主包解析且可长期缓存。
 */
export function setupIcons(app: App) {
  for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(name, component)
  }
}
