<script setup lang="ts">
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

// index.html 已做首屏主题预置以避免闪烁，这里以 store 为准再同步一次作为兜底
themeStore.apply()
</script>

<template>
  <!-- el-config-provider 提供语言与组件密度：原先挂在 app.use(ElementPlus) 上，
       改为按需引入后必须由该组件承载 -->
  <el-config-provider :locale="zhCn" :size="themeStore.size">
    <router-view />
  </el-config-provider>
</template>
