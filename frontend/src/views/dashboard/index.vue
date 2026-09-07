<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="dashboard">
    <el-card>
      <h2>欢迎回来，{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</h2>
      <p>这是 qkit 脚手架的首页占位。后续可对接仪表盘、统计图表等。</p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  padding: 16px;
}
</style>
