<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { pageRole, saveRole, deleteRole, getRoleDeptIds, getRoleMenuIds, assignMenu, type RoleItem, type RoleSave, type RoleQuery } from '@/api/system/role'
import { listDept } from '@/api/system/dept'
import { treeMenu, type MenuItem } from '@/api/system/menu'
import { usePermissionStore } from '@/stores/permission'
import { useCrud } from '@/composables/useCrud'

const permStore = usePermissionStore()
/** 无授权权限时不请求/不提交对应授权数据，避免只有「编辑角色」权限的角色被 403 打断 */
const canAssignMenu = computed(() => permStore.hasPermission('system:role:assign-menu'))
const canAssignDept = computed(() => permStore.hasPermission('system:role:assign-dept'))

const deptTree = ref<any[]>([])
const deptTreeRef = ref()
const menuTree = ref<MenuItem[]>([])
const menuTreeRef = ref()

const {
  query, list, total, loading,
  dialogVisible, dialogMode, form, formRef,
  fetch, onSearch, onReset, onAdd, onEdit, onSave, onDelete
} = useCrud<RoleItem, RoleQuery, RoleSave>({
  page: (q) => pageRole(q),
  save: (data) => saveRole(data),
  remove: (id) => deleteRole(Number(id)),
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, name: '', code: '', status: undefined }),
  defaultForm: () => ({ id: undefined, name: '', code: '', status: 1, dataScope: 2, sort: 0, remark: '', deptIds: [] }),
  toForm: (row) => ({
    id: row.id, name: row.name, code: row.code, status: row.status,
    dataScope: row.dataScope, sort: row.sort, remark: row.remark || '',
    deptIds: []
  }),
  afterAdd: () => {
    menuTreeRef.value?.setCheckedKeys([])
    deptTreeRef.value?.setCheckedKeys([])
  },
  afterEdit: async (row, form) => {
    // 菜单与自定义部门都不在角色主表上，需单独查询回显
    const [menuIds, deptIds] = await Promise.all([
      canAssignMenu.value ? getRoleMenuIds(row.id) : Promise.resolve<number[]>([]),
      row.dataScope === 5 && canAssignDept.value ? getRoleDeptIds(row.id) : Promise.resolve<number[]>([])
    ])
    form.deptIds = deptIds
    menuTreeRef.value?.setCheckedKeys(menuIds)
    deptTreeRef.value?.setCheckedKeys(deptIds)
  },
  beforeSave: (form) => {
    form.deptIds = form.dataScope === 5 ? ((deptTreeRef.value?.getCheckedKeys() ?? []) as number[]) : []
  },
  afterSave: async (saved, form) => {
    const roleId = typeof saved === 'number' ? saved : form.id
    // 菜单不在角色主表上，保存后单独提交；半选节点（目录）也要落库，否则父级菜单不会显示
    if (roleId && canAssignMenu.value) {
      const checked = (menuTreeRef.value?.getCheckedKeys() ?? []) as number[]
      const halfChecked = (menuTreeRef.value?.getHalfCheckedKeys() ?? []) as number[]
      await assignMenu(roleId, [...checked, ...halfChecked])
    }
  },
  confirmDelete: (row) => `确认删除角色「${row.name}」？`
})

async function loadDeptTree() {
  try {
    const items = await listDept()
    const map = new Map<number, any>()
    items.forEach(d => map.set(d.id, { ...d, children: [] as any[] }))
    const roots: any[] = []
    map.forEach((node, id) => {
      const parent = map.get(node.parentId)
      if (parent) (parent.children || (parent.children = [])).push(node)
      else roots.push(node)
    })
    deptTree.value = roots
  } catch (e) {
    console.warn('加载部门树失败', e)
    deptTree.value = []
  }
}

async function loadMenuTree() {
  try {
    menuTree.value = await treeMenu()
  } catch (e) {
    console.warn('加载菜单树失败', e)
    menuTree.value = []
  }
}

onMounted(() => {
  // 授权树按权限加载，避免无权限角色进入页面即请求 403
  if (canAssignDept.value) loadDeptTree()
  if (canAssignMenu.value) loadMenuTree()
})
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
        <el-form-item label="菜单权限" v-permission="'system:role:assign-menu'">
          <div class="tree-box">
            <el-tree
              ref="menuTreeRef"
              :data="menuTree"
              show-checkbox
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              default-expand-all
            />
          </div>
        </el-form-item>
        <el-form-item label="部门" v-if="form.dataScope===5" v-permission="'system:role:assign-dept'">
          <div class="tree-box">
            <el-tree
              ref="deptTreeRef"
              :data="deptTree"
              show-checkbox
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              default-expand-all
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tree-box {
  width: 100%;
  max-height: 240px;
  overflow: auto;
  border: 1px solid var(--el-border-color);
  border-radius: var(--app-radius-sm);
  padding: 4px 8px;
}
</style>
