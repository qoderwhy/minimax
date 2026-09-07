<script setup lang="ts">
import { computed, nextTick, onMounted, provide, ref } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 路由视图是否存活：控制刷新当前页（重新挂载 router-view 及其组件） */
const isRouterAlive = ref(true)

/** 刷新当前页：先卸载再重挂载，组件将重新执行初始化逻辑 */
function reload() {
  isRouterAlive.value = false
  nextTick(() => {
    isRouterAlive.value = true
  })
}

provide('reload', reload)

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {
      router.push('/login')
    })
  }
})
</script>

<template>
  <div class="app-wrapper">
    <Sidebar class="sidebar-container" />
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
}
.sidebar-container {
  width: 220px;
  background: #001529;
  color: #fff;
  overflow-y: auto;
}
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.app-main {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #f5f7fa;
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
