<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { pageUser, saveUser, deleteUser, resetUserPassword, updateUserStatus, exportUser, type UserItem, type UserQuery, type UserSave } from '@/api/system/user'
import { listRole, type RoleItem } from '@/api/system/role'
import { listDept, type DeptItem } from '@/api/system/dept'
import { listPost, type PostItem } from '@/api/system/post'
import { required, mobile, email } from '@/utils/validate'

const query = reactive<UserQuery>({ pageNum: 1, pageSize: 10, username: '', nickname: '', mobile: '', status: undefined, deptId: undefined })
const list = ref<UserItem[]>([])
const total = ref(0)
const loading = ref(false)

const roleList = ref<RoleItem[]>([])
const deptList = ref<DeptItem[]>([])
const postList = ref<PostItem[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<UserSave>({ id: undefined, username: '', nickname: '', password: '', mobile: '', email: '', status: 1, deptId: undefined, roleIds: [], postIds: [] })
const formRef = ref()

const formRules = computed(() => ({
  username: [required('请输入用户名')],
  nickname: [required('请输入昵称')],
  password: [...(dialogMode.value === 'add' ? [required('请输入密码')] : [])],
  mobile: [mobile()],
  email: [email()]
}))

async function fetch() {
  loading.value = true
  try {
    const res = await pageUser(query)
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
  query.username = ''
  query.nickname = ''
  query.mobile = ''
  query.status = undefined
  query.deptId = undefined
  query.pageNum = 1
  fetch()
}

async function loadOptions() {
  roleList.value = await listRole()
  deptList.value = await listDept()
  postList.value = await listPost()
}

function onAdd() {
  dialogMode.value = 'add'
  form.value = { id: undefined, username: '', nickname: '', password: '', mobile: '', email: '', status: 1, deptId: undefined, roleIds: [], postIds: [] }
  dialogVisible.value = true
}

async function onEdit(row: UserItem) {
  dialogMode.value = 'edit'
  // 简化：实际应调用 getUser 获取详情（含角色/岗位），这里使用列表数据近似
  form.value = {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    mobile: row.mobile || '',
    email: row.email || '',
    status: row.status,
    deptId: row.deptId,
    roleIds: [],
    postIds: []
  }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveUser(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetch()
}

async function onDelete(row: UserItem) {
  await ElMessageBox.confirm(`确认删除用户「${row.nickname}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  fetch()
}

async function onResetPwd(row: UserItem) {
  const { value } = await ElMessageBox.prompt('请输入新密码（6-20 位）', '重置密码', { inputPattern: /^\S{6,20}$/, inputErrorMessage: '密码格式不正确' })
  await resetUserPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function onStatusChange(row: UserItem) {
  await updateUserStatus(row.id, row.status)
  ElMessage.success('状态已更新')
}

async function onExport() {
  const resp = await exportUser(query)
  const blob = new Blob([resp.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const fileName = getFileName(resp) || '用户列表.xlsx'
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  window.URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

function getFileName(resp: any): string | null {
  const disposition = resp?.headers?.['content-disposition'] as string | undefined
  if (!disposition) return null
  const utf8 = disposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8) return decodeURIComponent(utf8[1])
  const name = disposition.match(/filename="?([^";]+)"?/i)
  return name ? decodeURIComponent(name[1]) : null
}

onMounted(() => {
  loadOptions()
  fetch()
})
</script>

<template>
  <div class="page">
    <el-card class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="用户名"><el-input v-model="query.username" clearable /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="query.nickname" clearable /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="query.mobile" clearable /></el-form-item>
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
        <el-button type="primary" v-permission="'system:user:create'" @click="onAdd">新增用户</el-button>
        <el-button type="success" v-permission="'system:user:export'" @click="onExport">导出</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="mobile" label="手机号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="deptName" label="部门" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="onStatusChange(row as UserItem)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button v-permission="'system:user:update'" type="primary" link @click="onEdit(row as UserItem)">编辑</el-button>
            <el-button v-permission="'system:user:reset-password'" type="warning" link @click="onResetPwd(row as UserItem)">重置密码</el-button>
            <el-button v-permission="'system:user:delete'" type="danger" link @click="onDelete(row as UserItem)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增用户' : '编辑用户'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'add'" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="form.deptId" :data="deptList" :props="{ value: 'id', label: 'name' } as any" check-strictly placeholder="选择部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.postIds" multiple style="width: 100%">
            <el-option v-for="p in postList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
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
