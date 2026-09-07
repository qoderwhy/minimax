<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { treeMenu, saveMenu, deleteMenu, type MenuItem, type MenuSave } from '@/api/system/menu'

const list = ref<MenuItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<MenuSave>({ id: '', parentId: '0', name: '', type: 1, path: '', component: '', permCode: '', icon: '', sort: 0, status: 1, visible: 1, keepAlive: 0 })
const formRef = ref()

async function fetch() {
  loading.value = true
  try {
    list.value = await treeMenu()
  } finally {
    loading.value = false
  }
}

function onAdd() {
  dialogMode.value = 'add'
  form.value = { id: '', parentId: '0', name: '', type: 1, path: '', component: '', permCode: '', icon: '', sort: 0, status: 1, visible: 1, keepAlive: 0 }
  dialogVisible.value = true
}

function onAddChild(parent: MenuItem) {
  dialogMode.value = 'add'
  form.value = { id: '', parentId: parent.id, name: '', type: 2, path: '', component: '', permCode: '', icon: '', sort: 0, status: 1, visible: 1, keepAlive: 0 }
  dialogVisible.value = true
}

function onEdit(row: MenuItem) {
  dialogMode.value = 'edit'
  form.value = {
    id: row.id,
    parentId: row.parentId,
    name: row.name,
    type: row.type,
    path: row.path || '',
    component: row.component || '',
    permCode: row.permCode || '',
    icon: row.icon || '',
    sort: row.sort,
    status: row.status,
    visible: row.visible,
    keepAlive: row.keepAlive || 0
  }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveMenu(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetch()
}

async function onDelete(row: MenuItem) {
  await ElMessageBox.confirm(`确认删除菜单「${row.name}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  fetch()
}

const typeLabel = (t: number) => ['', '目录', '菜单', '按钮'][t] || '-'

onMounted(fetch)
</script>

<template>
  <div class="page">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" v-permission="'system:menu:save'" @click="onAdd">新增根菜单</el-button>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id" :tree-props="{ children: 'children' }" default-expand-all border>
        <el-table-column prop="name" label="菜单名称" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }"><el-tag>{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="80" />
        <el-table-column prop="path" label="路由" />
        <el-table-column prop="component" label="组件" />
        <el-table-column prop="permCode" label="权限标识" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button v-permission="'system:menu:save'" type="primary" link @click="onAddChild(row as MenuItem)">新增</el-button>
            <el-button v-permission="'system:menu:update'" type="primary" link @click="onEdit(row as MenuItem)">编辑</el-button>
            <el-button v-permission="'system:menu:delete'" type="danger" link @click="onDelete(row as MenuItem)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'edit' ? '编辑菜单' : '新增菜单'" width="600px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="form.parentId" :data="list" :props="{ value: 'id', label: 'name' } as any" check-strictly placeholder="根菜单" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="form.type !== 3" label="图标"><el-input v-model="form.icon" placeholder="如：User" /></el-form-item>
        <el-form-item v-if="form.type !== 3" label="路由"><el-input v-model="form.path" placeholder="如：user" /></el-form-item>
        <el-form-item v-if="form.type === 2" label="组件"><el-input v-model="form.component" placeholder="如：system/user/index" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.permCode" placeholder="如：system:user:save" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item v-if="form.type === 2" label="显示">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="缓存">
          <el-radio-group v-model="form.keepAlive">
            <el-radio :value="1">缓存</el-radio>
            <el-radio :value="0">不缓存</el-radio>
          </el-radio-group>
        </el-form-item>
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
