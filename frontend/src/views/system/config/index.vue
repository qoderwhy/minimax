<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { deleteConfig, pageConfig, saveConfig, type SysConfig } from '@/api/system/config'

const query = reactive({ pageNum: 1, pageSize: 10, configKey: '', configName: '' })
const list = ref<SysConfig[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<SysConfig>({ id: '', configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
const formRef = ref()

async function fetchList() {
  loading.value = true
  try {
    const res = await pageConfig(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  fetchList()
}

function onAdd() {
  dialogMode.value = 'add'
  form.value = { id: '', configName: '', configKey: '', configValue: '', configType: 'N', remark: '' }
  dialogVisible.value = true
}

function onEdit(row: SysConfig) {
  dialogMode.value = 'edit'
  form.value = { ...row }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveConfig(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchList()
}

async function onDelete(row: SysConfig) {
  if (row.configType === 'Y') {
    ElMessage.warning('系统内置参数不允许删除')
    return
  }
  await ElMessageBox.confirm(`确认删除参数「${row.configName}」？`, '提示', { type: 'warning' })
  await deleteConfig(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="page">
    <el-card>
      <el-form :model="query" inline class="search-form">
        <el-form-item label="参数名称">
          <el-input v-model="query.configName" placeholder="参数名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="参数键名">
          <el-input v-model="query.configKey" placeholder="参数键名" clearable style="width: 200px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button v-permission="'system:config:create'" type="success" @click="onAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border size="small">
        <el-table-column prop="configName" label="参数名称" min-width="150" />
        <el-table-column prop="configKey" label="参数键名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" label="参数键值" min-width="200" show-overflow-tooltip />
        <el-table-column label="系统内置" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.configType === 'Y' ? 'danger' : 'info'">{{ row.configType === 'Y' ? '内置' : '自定义' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:config:update'" type="primary" link size="small" @click="onEdit(row as SysConfig)">编辑</el-button>
            <el-button v-permission="'system:config:delete'" type="danger" link size="small" @click="onDelete(row as SysConfig)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        small
        style="margin-top: 8px; justify-content: flex-end"
        @current-change="fetchList"
        @size-change="fetchList"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增参数' : '编辑参数'" width="520px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="参数名称" prop="configName"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" :disabled="dialogMode === 'edit' && form.configType === 'Y'" placeholder="如 sys.upload.maxSize" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue"><el-input v-model="form.configValue" /></el-form-item>
        <el-form-item label="系统内置">
          <el-radio-group v-model="form.configType">
            <el-radio value="N">否</el-radio>
            <el-radio value="Y">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.search-form {
  margin-bottom: 8px;
}
</style>