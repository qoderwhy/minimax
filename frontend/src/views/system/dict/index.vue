<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageDictType, saveDictType, deleteDictType, type DictType } from '@/api/system/dict'

const router = useRouter()

const query = reactive({ pageNum: 1, pageSize: 10, type: '', name: '', status: undefined as number | undefined })
const list = ref<DictType[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<DictType>({ id: '', type: '', name: '', status: 1, remark: '' })
const formRef = ref()

async function fetch() {
  loading.value = true
  try {
    const res = await pageDictType(query)
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

function onAdd() {
  dialogMode.value = 'add'
  form.value = { id: '', type: '', name: '', status: 1, remark: '' }
  dialogVisible.value = true
}

function onEdit(row: DictType) {
  dialogMode.value = 'edit'
  form.value = { ...row }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveDictType(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetch()
}

async function onDelete(row: DictType) {
  await ElMessageBox.confirm(`确认删除字典「${row.name}」？`, '提示', { type: 'warning' })
  await deleteDictType(row.id)
  ElMessage.success('删除成功')
  fetch()
}

function onItems(row: DictType) {
  router.push({ path: '/system/dict/items', query: { type: row.type } })
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <el-card class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="字典名称"><el-input v-model="query.name" clearable /></el-form-item>
        <el-form-item label="字典类型"><el-input v-model="query.type" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-button type="primary" v-permission="'system:dict:save'" @click="onAdd">新增字典</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="字典名称" />
        <el-table-column prop="type" label="字典类型" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-permission="'system:dict:list'" type="info" link @click="onItems(row as DictType)">数据项</el-button>
            <el-button v-permission="'system:dict:update'" type="primary" link @click="onEdit(row as DictType)">编辑</el-button>
            <el-button v-permission="'system:dict:delete'" type="danger" link @click="onDelete(row as DictType)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增字典' : '编辑字典'" width="500px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="字典名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="字典类型" prop="type"><el-input v-model="form.type" :disabled="dialogMode === 'edit'" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
