<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { pageRole, saveRole, deleteRole, type RoleItem, type RoleSave, type RoleQuery } from '@/api/system/role'

const query = reactive<RoleQuery>({ pageNum: 1, pageSize: 10, name: '', code: '', status: undefined })
const list = ref<RoleItem[]>([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<RoleSave>({ id: undefined, name: '', code: '', status: 1, dataScope: 1, sort: 0, remark: '', menuIds: [] })
const formRef = ref()

async function fetch() {
  loading.value = true
  try {
    const res = await pageRole(query)
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

function onReset() {
  query.name = ''
  query.code = ''
  query.status = undefined
  query.pageNum = 1
  fetch()
}

function onAdd() {
  dialogMode.value = 'add'
  form.value = { id: undefined, name: '', code: '', status: 1, dataScope: 1, sort: 0, remark: '', menuIds: [] }
  dialogVisible.value = true
}

function onEdit(row: RoleItem) {
  dialogMode.value = 'edit'
  form.value = {
    id: row.id,
    name: row.name,
    code: row.code,
    status: row.status,
    dataScope: row.dataScope,
    sort: row.sort,
    remark: row.remark || '',
    menuIds: []
  }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveRole(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetch()
}

async function onDelete(row: RoleItem) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <el-card class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="角色名"><el-input v-model="query.name" clearable /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="query.code" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-button type="primary" v-permission="'system:role:create'" @click="onAdd">新增角色</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="角色名" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column label="数据权限" width="120">
          <template #default="{ row }">
            <el-tag>{{ ['', '全部', '本部门及下级', '本部门', '本人', '自定义'][row.dataScope] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-permission="'system:role:update'" type="primary" link @click="onEdit(row as RoleItem)">编辑</el-button>
            <el-button v-permission="'system:role:delete'" type="danger" link @click="onDelete(row as RoleItem)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增角色' : '编辑角色'" width="600px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="角色名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色编码" prop="code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="数据权限">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" :value="1" />
            <el-option label="本部门及下级" :value="2" />
            <el-option label="本部门" :value="3" />
            <el-option label="仅本人" :value="4" />
            <el-option label="自定义" :value="5" />
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
