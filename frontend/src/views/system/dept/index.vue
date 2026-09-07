<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { treeDept, saveDept, deleteDept, type DeptItem, type DeptSave } from '@/api/system/dept'

const list = ref<DeptItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit' | 'addChild'>('add')
const form = ref<DeptSave>({ id: '', name: '', parentId: '0', sort: 0, leader: '', mobile: '', email: '', status: 1 })
const formRef = ref()

async function fetch() {
  loading.value = true
  try {
    list.value = await treeDept()
  } finally {
    loading.value = false
  }
}

function onAdd(root = true) {
  dialogMode.value = 'add'
  form.value = { id: '', name: '', parentId: root ? '0' : '0', sort: 0, leader: '', mobile: '', email: '', status: 1 }
  dialogVisible.value = true
}

function onAddChild(parent: DeptItem) {
  dialogMode.value = 'addChild'
  form.value = { id: '', name: '', parentId: parent.id, sort: 0, leader: '', mobile: '', email: '', status: 1 }
  dialogVisible.value = true
}

function onEdit(row: DeptItem) {
  dialogMode.value = 'edit'
  form.value = {
    id: row.id,
    name: row.label,
    parentId: row.parentId,
    sort: row.sort ?? 0,
    leader: row.leader || '',
    mobile: row.mobile || '',
    email: row.email || '',
    status: row.status ?? 1
  }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveDept(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetch()
}

async function onDelete(row: DeptItem) {
  await ElMessageBox.confirm(`确认删除部门「${row.label}」？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" v-permission="'system:dept:save'" @click="onAdd(true)">新增根部门</el-button>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id" :tree-props="{ children: 'children' }" default-expand-all border>
        <el-table-column prop="label" label="部门名称" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="mobile" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button v-permission="'system:dept:save'" type="primary" link @click="onAddChild(row as DeptItem)">新增下级</el-button>
            <el-button v-permission="'system:dept:update'" type="primary" link @click="onEdit(row as DeptItem)">编辑</el-button>
            <el-button v-permission="'system:dept:delete'" type="danger" link @click="onDelete(row as DeptItem)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'edit' ? '编辑部门' : '新增部门'" width="600px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="list" :props="{ value: 'id', label: 'label' } as any" check-strictly placeholder="根部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
