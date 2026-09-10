<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref } from 'vue'
import { pageOperLog, deleteOperLog, cleanOperLog } from '@/api/system/operLog'
import { usePagination } from '@/composables/usePagination'

type OperLogQuery = {
  pageNum: number
  pageSize: number
  module: string
  username: string
  status?: number
}

/** 时间范围，提交时拆分为 beginTime / endTime */
const dateRange = ref<any>(null)

const { query, list, total, loading, fetch, onSearch } = usePagination<any, OperLogQuery>({
  page: (q) => pageOperLog({ ...q, beginTime: dateRange.value?.[0], endTime: dateRange.value?.[1] }),
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, module: '', username: '', status: undefined })
})

const detailVisible = ref(false)
const detail = ref<any>(null)

function onView(row: any) {
  detail.value = row
  detailVisible.value = true
}

async function onDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该日志？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteOperLog(row.id)
  ElMessage.success('删除成功')
  fetch()
}

async function onClean() {
  try {
    await ElMessageBox.confirm('确认清空全部操作日志？此操作不可恢复！', '提示', { type: 'warning' })
  } catch {
    return
  }
  await cleanOperLog()
  ElMessage.success('已清空')
  fetch()
}
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
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
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
        <el-table-column prop="username" label="操作人" width="120">
          <template #default="{ row }">{{ row.username || '--' }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" />
        <el-table-column prop="operTime" label="时间" width="170" />
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
        <el-descriptions-item label="操作人">{{ detail?.username || '--' }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail?.ip }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail?.method }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ detail?.costMs }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail?.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2"><pre style="white-space: pre-wrap; word-break: break-all; margin: 0;">{{ detail?.requestParam }}</pre></el-descriptions-item>
        <el-descriptions-item label="响应" :span="2"><pre style="white-space: pre-wrap; word-break: break-all; margin: 0;">{{ detail?.responseResult }}</pre></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
