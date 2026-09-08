<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const permStore = usePermissionStore()

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
  <el-menu :default-active="activeMenu" router background-color="#001529" text-color="#fff" active-text-color="#409eff">
    <div class="logo">qkit</div>
    <template v-for="m in menus" :key="m.path">
      <el-sub-menu v-if="m.children && m.children.length > 1" :index="m.path">
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
      <el-menu-item v-else-if="m.children && m.children.length === 1" :index="resolvePath(m, m.children[0])">
        <el-icon v-if="m.meta?.icon"><component :is="m.meta.icon" /></el-icon>
        <span>{{ m.children[0].meta?.title || m.meta?.title }}</span>
      </el-menu-item>
      <el-menu-item v-else-if="!m.children || m.children.length === 0" :index="m.path">
        <el-icon v-if="m.meta?.icon"><component :is="m.meta.icon" /></el-icon>
        <span>{{ m.meta?.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped lang="scss">
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: bold;
  color: #fff;
  background: #002140;
}
:deep(.el-menu) {
  border-right: 0;
}
</style>
