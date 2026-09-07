<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { pageOperLog, deleteOperLog, cleanOperLog } from '@/api/system/operLog'

const query = reactive({ pageNum: 1, pageSize: 10, module: '', username: '', status: undefined as number | undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)

async function fetch() {
  loading.value = true
  try {
    const res = await pageOperLog(query)
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

function onView(row: any) {
  detail.value = row
  detailVisible.value = true
}

async function onDelete(row: any) {
  await ElMessageBox.confirm('确认删除该日志？', '提示', { type: 'warning' })
  await deleteOperLog(row.id)
  ElMessage.success('删除成功')
  fetch()
}

async function onClean() {
  await ElMessageBox.confirm('确认清空全部操作日志？此操作不可恢复！', '提示', { type: 'warning' })
  await cleanOperLog()
  ElMessage.success('已清空')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <el-card class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="模块"><el-input v-model="query.module" clearable /></el-form-item>
        <el-form-item label="操作人"><el-input v-model="query.username" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button type="danger" v-permission="'system:oper-log:clean'" @click="onClean">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="module" label="模块" />
        <el-table-column prop="name" label="操作" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="onView(row)">详情</el-button>
            <el-button v-permission="'system:oper-log:delete'" type="danger" link @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="detailVisible" title="日志详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模块">{{ detail?.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail?.name }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail?.username }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail?.ip }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail?.method }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ detail?.costMs }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail?.url }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2"><pre>{{ detail?.reqParam }}</pre></el-descriptions-item>
        <el-descriptions-item label="响应" :span="2"><pre>{{ detail?.resp }}</pre></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
