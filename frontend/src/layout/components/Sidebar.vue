<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const permStore = usePermissionStore()
const appStore = useAppStore()

function resolvePath(parent: any, child: any) {
  return child.path.startsWith('/') ? child.path : `${parent.path}/${child.path}`
}

const menus = computed(() => {
  const staticMenus = router.options.routes.filter(
    (r) => r.meta?.title && !r.meta?.hidden && r.path !== '/login' && r.path !== '/404'
  )
  const dynamicMenus = permStore.routes.filter((r) => r.meta?.title && !r.meta?.hidden)
  return [...staticMenus, ...dynamicMenus]
})

const activeMenu = computed(() => route.path)
</script>

<template>
  <el-menu
    class="sidebar-menu"
    :default-active="activeMenu"
    :collapse="appStore.sidebarCollapsed"
    :collapse-transition="false"
    router
  >
    <div class="logo">
      <span class="logo-mark">q</span>
      <span v-show="!appStore.sidebarCollapsed" class="logo-text">qkit</span>
    </div>

    <template v-for="m in menus" :key="m.path">
      <el-sub-menu v-if="m.children && m.children.length > 0" :index="m.path">
        <template #title>
          <el-icon v-if="m.meta?.icon"><component :is="m.meta.icon" /></el-icon>
          <span>{{ m.meta?.title }}</span>
        </template>
        <el-menu-item
          v-for="c in m.children.filter((c: any) => c.meta?.title && !c.meta?.hidden)"
          :key="c.path"
          :index="resolvePath(m, c)"
        >
          <el-icon v-if="c.meta?.icon"><component :is="c.meta.icon" /></el-icon>
          <span>{{ c.meta?.title }}</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item v-else :index="m.path">
        <el-icon v-if="m.meta?.icon"><component :is="m.meta.icon" /></el-icon>
        <span>{{ m.meta?.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped lang="scss">
.sidebar-menu {
  height: 100%;
  border-right: none;

  /* 侧边栏配色统一交给菜单变量，避免逐个选择器覆盖；
     这些变量随主题切换（themes.scss 中的 --app-sidebar-*）而变化 */
  --el-menu-bg-color: var(--app-sidebar-bg);
  --el-menu-text-color: var(--app-sidebar-text);
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-active-color: #ffffff;
  --el-menu-item-height: 46px;
  --el-menu-sub-item-height: 42px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: var(--app-navbar-height);
  overflow: hidden;
  background: var(--app-sidebar-logo-bg);

  .logo-mark {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    font-size: 16px;
    font-weight: 700;
    color: #fff;
    background: var(--el-color-primary);
    border-radius: var(--app-radius-sm);
  }

  .logo-text {
    font-size: 19px;
    font-weight: 700;
    letter-spacing: 1px;
    color: #fff;
    white-space: nowrap;
  }
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  &:hover {
    color: #fff;
  }
}

/* 激活项使用主题色实色背景，切换主题时高亮同步变化；同时覆盖 hover 态避免被冲淡 */
:deep(.el-menu-item.is-active),
:deep(.el-menu-item.is-active:hover) {
  color: #fff;
  background-color: var(--el-color-primary);
}
</style>
