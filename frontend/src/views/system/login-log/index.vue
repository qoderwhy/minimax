<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { pageLoginLog, deleteLoginLog, cleanLoginLog } from '@/api/system/loginLog'

const query = reactive({ pageNum: 1, pageSize: 10, username: '', status: undefined as number | undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function fetch() {
  loading.value = true
  try {
    const res = await pageLoginLog(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  fetch()
}

async function onDelete(row: any) {
  await ElMessageBox.confirm('确认删除该日志？', '提示', { type: 'warning' })
  await deleteLoginLog(row.id)
  ElMessage.success('删除成功')
  fetch()
}

async function onClean() {
  await ElMessageBox.confirm('确认清空全部登录日志？此操作不可恢复！', '提示', { type: 'warning' })
  await cleanLoginLog()
  ElMessage.success('已清空')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <el-card class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="用户名"><el-input v-model="query.username" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="成功" :value="0" />
            <el-option label="失败" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button type="danger" v-permission="'system:login-log:clean'" @click="onClean">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" />
        <el-table-column prop="loginTime" label="时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-permission="'system:login-log:delete'" type="danger" link @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="fetch"
        @size-change="fetch"
      />
    </el-card>
  </div>
</template>
