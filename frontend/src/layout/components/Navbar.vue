<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useThemeStore } from '@/stores/theme'
import ThemePanel from '@/components/ThemePanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const themeStore = useThemeStore()

const themeVisible = ref(false)
const isFullscreen = ref(false)

/** 面包屑：取当前匹配链上声明了 title 的路由 */
const breadcrumbs = computed(() =>
  route.matched.filter((item) => item.meta?.title).map((item) => String(item.meta.title))
)

function onProfile() {
  router.push('/profile')
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await userStore.logout()
  router.push('/login')
}

async function toggleFullscreen() {
  if (document.fullscreenElement) {
    await document.exitFullscreen()
  } else {
    await document.documentElement.requestFullscreen()
  }
}

/** 用户按 Esc 退出全屏时同步图标状态 */
function syncFullscreen() {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

onMounted(() => document.addEventListener('fullscreenchange', syncFullscreen))
onBeforeUnmount(() => document.removeEventListener('fullscreenchange', syncFullscreen))
</script>

<template>
  <div class="navbar">
    <div class="left">
      <el-icon class="action" title="折叠 / 展开菜单" @click="appStore.toggleSidebar()">
        <component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
      </el-icon>

      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(title, index) in breadcrumbs" :key="index">
          {{ title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right">
      <el-icon
        class="action"
        :title="themeStore.dark ? '切换为浅色模式' : '切换为深色模式'"
        @click="themeStore.toggleDark()"
      >
        <component :is="themeStore.dark ? 'Sunny' : 'Moon'" />
      </el-icon>

      <el-icon class="action" :title="isFullscreen ? '退出全屏' : '全屏显示'" @click="toggleFullscreen">
        <component :is="isFullscreen ? 'ScaleToOriginal' : 'FullScreen'" />
      </el-icon>

      <el-icon class="action" title="主题设置" @click="themeVisible = true">
        <Brush />
      </el-icon>

      <el-dropdown>
        <span class="user-info">
          <el-avatar :size="28" :src="userStore.userInfo?.avatar || ''" />
          <span class="user-name">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
          </span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="onProfile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item divided @click="onLogout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <ThemePanel v-model="themeVisible" />
  </div>
</template>

<style scoped lang="scss">
.navbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  height: var(--app-navbar-height);
  padding: 0 var(--app-space-md);
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);

  .left,
  .right {
    display: flex;
    align-items: center;
  }

  .left {
    gap: var(--app-space-sm);
    min-width: 0;
  }

  .right {
    gap: 2px;
  }
}

.action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: 18px;
  color: var(--el-text-color-regular);
  border-radius: var(--app-radius-sm);
  cursor: pointer;
  transition: all var(--app-transition);

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--app-space-xs);
  padding: 4px 8px;
  color: var(--el-text-color-regular);
  border-radius: var(--app-radius-sm);
  cursor: pointer;
  transition: all var(--app-transition);

  &:hover {
    background: var(--el-fill-color-light);
  }
}

:deep(.el-breadcrumb) {
  font-size: 13px;
}

@media (max-width: 768px) {
  .left :deep(.el-breadcrumb) {
    display: none;
  }

  .user-name {
    display: none;
  }
}
</style>
