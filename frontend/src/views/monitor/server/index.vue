<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getServerInfo, type ServerInfo } from '@/api/monitor/server'

const data = ref<ServerInfo | null>(null)
const loading = ref(false)

/** 将字节转换为可读格式 */
function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + units[i]
}

/** 格式化百分比 */
function formatPercent(value: number): string {
  return value.toFixed(2) + '%'
}

async function fetchData() {
  loading.value = true
  try {
    data.value = await getServerInfo()
  } finally {
    loading.value = false
  }
}

let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchData()
  // 每 3 秒自动刷新
  timer = setInterval(fetchData, 3000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div v-loading="loading" class="page">
    <template v-if="data">
      <!-- CPU -->
      <el-card shadow="never">
        <template #header>
          <span class="card-title">CPU</span>
        </template>
        <el-table :data="[
          { prop: '核心数', value: data.cpu?.cores },
          { prop: '用户使用率', value: formatPercent(data.cpu?.userUsage ?? 0) },
          { prop: '系统使用率', value: formatPercent(data.cpu?.systemUsage ?? 0) },
          { prop: '当前空闲率', value: formatPercent(data.cpu?.freeRate ?? 0) }
        ]" border stripe>
          <el-table-column prop="prop" label="属性" width="180" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </el-card>

      <!-- 内存 -->
      <el-card shadow="never">
        <template #header>
          <span class="card-title">内存</span>
        </template>
        <el-table :data="[
          { prop: '总内存', memory: formatBytes(data.memory?.totalMemory ?? 0), jvm: formatBytes(data.memory?.jvmTotalMemory ?? 0) },
          { prop: '已用内存', memory: formatBytes(data.memory?.usedMemory ?? 0), jvm: formatBytes(data.memory?.jvmUsedMemory ?? 0) },
          { prop: '剩余内存', memory: formatBytes(data.memory?.freeMemory ?? 0), jvm: formatBytes(data.memory?.jvmFreeMemory ?? 0) },
          { prop: '使用率', memory: formatPercent(data.memory?.usageRate ?? 0), jvm: formatPercent(data.memory?.jvmUsageRate ?? 0) }
        ]" border stripe>
          <el-table-column prop="prop" label="属性" width="180" />
          <el-table-column prop="memory" label="内存" />
          <el-table-column prop="jvm" label="JVM" />
        </el-table>
      </el-card>

      <!-- 服务器信息 -->
      <el-card shadow="never">
        <template #header>
          <span class="card-title">服务器信息</span>
        </template>
        <el-table :data="[
          { prop: '服务器名称', value: data.server?.name },
          { prop: '服务器IP', value: data.server?.ip },
          { prop: '操作系统', value: data.server?.os },
          { prop: '系统架构', value: data.server?.arch }
        ]" border stripe>
          <el-table-column prop="prop" label="属性" width="180" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </el-card>

      <!-- Java虚拟机信息 -->
      <el-card shadow="never">
        <template #header>
          <span class="card-title">Java虚拟机信息</span>
        </template>
        <el-table :data="[
          { prop: 'Java名称', value: data.jvm?.javaName },
          { prop: 'Java版本', value: data.jvm?.javaVersion },
          { prop: '启动时间', value: data.jvm?.startTime },
          { prop: '运行时长', value: data.jvm?.runTime },
          { prop: '安装路径', value: data.jvm?.installPath },
          { prop: '项目路径', value: data.jvm?.projectPath },
          { prop: '运行参数', value: data.jvm?.runArgs }
        ]" border stripe>
          <el-table-column prop="prop" label="属性" width="180" />
          <el-table-column prop="value" label="值" show-overflow-tooltip />
        </el-table>
      </el-card>

      <!-- 磁盘状态 -->
      <el-card shadow="never">
        <template #header>
          <span class="card-title">磁盘状态</span>
        </template>
        <el-table :data="data.disks ?? []" border stripe>
          <el-table-column prop="path" label="盘符路径" />
          <el-table-column prop="fileSystem" label="文件系统" />
          <el-table-column prop="type" label="盘符类型" />
          <el-table-column label="总大小">
            <template #default="{ row }">{{ formatBytes(row.totalSize) }}</template>
          </el-table-column>
          <el-table-column label="可用大小">
            <template #default="{ row }">{{ formatBytes(row.freeSize) }}</template>
          </el-table-column>
          <el-table-column label="已用大小">
            <template #default="{ row }">{{ formatBytes(row.usedSize) }}</template>
          </el-table-column>
          <el-table-column label="已用百分比">
            <template #default="{ row }">{{ formatPercent(row.usageRate) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card-title {
  font-weight: 600;
}
</style>
