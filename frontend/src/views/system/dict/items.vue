<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pageDictItem, saveDictItem, deleteDictItem, type DictItem, type DictType } from '@/api/system/dict'

const route = useRoute()
const router = useRouter()

const query = reactive({ pageNum: 1, pageSize: 10, type: '', label: '' })
const list = ref<DictItem[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<DictItem>({ id: '', dictType: '', label: '', value: '', sort: 0, status: 1, cssClass: 'primary', remark: '' })
const formRef = ref()

const typeList = ref<DictType[]>([])

async function fetch() {
  loading.value = true
  try {
    const res = await pageDictItem(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadTypes() {
  // 简化：直接通过 sys_dict 表的 page 接口拿到所有类型
  const res = await import('@/api/system/dict').then((m) => m.pageDictType({ pageNum: 1, pageSize: 100 }))
  typeList.value = res.list
  if (!query.type && res.list.length > 0) {
    query.type = res.list[0].type
  }
}

function onSearch() {
  query.pageNum = 1
  fetch()
}

function onAdd() {
  dialogMode.value = 'add'
  form.value = { id: '', dictType: query.type || '', label: '', value: '', sort: 0, status: 1, cssClass: 'primary', remark: '' }
  dialogVisible.value = true
}

function onEdit(row: DictItem) {
  dialogMode.value = 'edit'
  form.value = { ...row }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveDictItem(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetch()
}

async function onDelete(row: DictItem) {
  await ElMessageBox.confirm(`确认删除字典项「${row.label}」？`, '提示', { type: 'warning' })
  await deleteDictItem(row.id)
  ElMessage.success('删除成功')
  fetch()
}

onMounted(async () => {
  if (route.query.type) {
    query.type = String(route.query.type)
  }
  await loadTypes()
  fetch()
})
</script>

<template>
  <div class="page">
    <el-card class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="字典类型">
          <el-select v-model="query.type" clearable placeholder="全部" style="width: 200px">
            <el-option v-for="t in typeList" :key="t.id" :label="`${t.name} (${t.type})`" :value="t.type" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="query.label" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" v-permission="'system:dict:save'" @click="onAdd">新增字典项</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="dictType" label="字典类型" />
        <el-table-column prop="label" label="标签" />
        <el-table-column prop="value" label="值" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-permission="'system:dict:update'" type="primary" link @click="onEdit(row as DictItem)">编辑</el-button>
            <el-button v-permission="'system:dict:delete'" type="danger" link @click="onDelete(row as DictItem)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增字典项' : '编辑字典项'" width="500px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="字典类型" prop="dictType">
          <el-select v-model="form.dictType" style="width: 100%">
            <el-option v-for="t in typeList" :key="t.id" :label="t.type" :value="t.type" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="label"><el-input v-model="form.label" /></el-form-item>
        <el-form-item label="值" prop="value"><el-input v-model="form.value" /></el-form-item>
        <el-form-item label="样式">
          <el-select v-model="form.cssClass" style="width: 100%">
            <el-option label="primary" value="primary" />
            <el-option label="success" value="success" />
            <el-option label="warning" value="warning" />
            <el-option label="info" value="info" />
            <el-option label="danger" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
