<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, provide, ref } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

/** 路由视图是否存活：控制刷新当前页（重新挂载 router-view 及其组件） */
const isRouterAlive = ref(true)

/** 窄于该宽度时自动折叠侧边栏，避免内容区被压缩 */
const MOBILE_BREAKPOINT = 992

const sidebarWidth = computed(() =>
  appStore.sidebarCollapsed ? 'var(--app-sidebar-width-collapsed)' : 'var(--app-sidebar-width)'
)

/** 刷新当前页：先卸载再重挂载，组件将重新执行初始化逻辑 */
function reload() {
  isRouterAlive.value = false
  nextTick(() => {
    isRouterAlive.value = true
  })
}

function syncViewport() {
  if (window.innerWidth < MOBILE_BREAKPOINT) {
    appStore.setSidebarCollapsed(true)
  }
}

provide('reload', reload)

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)

  if (!userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {
      router.push('/login')
    })
  }
})

onBeforeUnmount(() => window.removeEventListener('resize', syncViewport))
</script>

<template>
  <div class="app-wrapper">
    <Sidebar class="sidebar-container" :style="{ width: sidebarWidth }" />
    <div class="main-container">
      <Navbar />
      <TagsView />
      <div class="app-main">
        <router-view v-if="isRouterAlive" v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive>
              <component :is="Component" :key="route.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-wrapper {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar-container {
  flex-shrink: 0;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--app-sidebar-bg);
  transition: width var(--app-transition);
}

.main-container {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.app-main {
  flex: 1;
  padding: var(--app-space-md);
  overflow-y: auto;
  background: var(--el-bg-color-page);
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
