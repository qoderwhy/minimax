<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { pageUser, getUser, saveUser, deleteUser, resetUserPassword, updateUserStatus, exportUser, assignRole, type UserItem, type UserQuery, type UserSave } from '@/api/system/user'
import { listRole, type RoleItem } from '@/api/system/role'
import { listDept, type DeptItem } from '@/api/system/dept'
import { listPost, type PostItem } from '@/api/system/post'
import { required, mobile, email } from '@/utils/validate'
import { usePermissionStore } from '@/stores/permission'

const query = reactive<UserQuery>({ pageNum: 1, pageSize: 10, username: '', nickname: '', phone: '', status: undefined, deptId: undefined })
const list = ref<UserItem[]>([])
const total = ref(0)
const loading = ref(false)

const roleList = ref<RoleItem[]>([])
const deptList = ref<DeptItem[]>([])
const postList = ref<PostItem[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<UserSave>({ id: undefined, username: '', nickname: '', password: '', phone: '', email: '', status: 1, deptId: undefined, postId: undefined, roleIds: [] })
const formRef = ref()

const permStore = usePermissionStore()
/** 无对应权限时不发起详情/授权请求，避免只有「编辑」权限的角色被 403 打断保存流程 */
const canViewUserDetail = computed(() => permStore.hasPermission('system:user:detail'))
const canAssignRole = computed(() => permStore.hasPermission('system:user:assign-role'))

const formRules = computed(() => ({
  username: [required('请输入用户名')],
  nickname: [required('请输入昵称')],
  password: [
    ...(dialogMode.value === 'add'
      ? [required('请输入密码'), { min: 8, max: 32, message: '密码长度必须在8-32位之间', trigger: 'blur' as const }]
      : [])
  ],
  phone: [mobile()],
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
  query.phone = ''
  query.status = undefined
  query.deptId = undefined
  query.pageNum = 1
  fetch()
}

async function loadOptions() {
  // 逐个容错加载：某个下拉因权限不足失败，不应连带其余下拉一起加载不出来
  await Promise.all([
    canAssignRole.value
      ? listRole().then((r) => { roleList.value = r }).catch(() => {})
      : Promise.resolve(),
    listDept().then((d) => { deptList.value = d }).catch(() => {}),
    listPost().then((p) => { postList.value = p }).catch(() => {})
  ])
}

function onAdd() {
  dialogMode.value = 'add'
  form.value = { id: undefined, username: '', nickname: '', password: '', phone: '', email: '', status: 1, deptId: undefined, postId: undefined, roleIds: [] }
  dialogVisible.value = true
}

async function onEdit(row: UserItem) {
  dialogMode.value = 'edit'
  // 列表数据不含已分配角色，需取详情回显；无详情权限时退化为使用列表行数据
  const detail = canViewUserDetail.value ? await getUser(row.id) : row
  form.value = {
    id: detail.id,
    username: detail.username,
    nickname: detail.nickname,
    phone: detail.phone || '',
    email: detail.email || '',
    status: detail.status,
    deptId: detail.deptId,
    postId: detail.postId,
    roleIds: detail.roleIds ?? []
  }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const res = await saveUser(form.value)
  // 角色不在用户主表上，保存后单独调用分配角色接口（新增接口返回新用户 ID）
  const userId = typeof res === 'number' ? res : form.value.id
  if (userId && canAssignRole.value) await assignRole(userId, form.value.roleIds ?? [])
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
  const { value } = await ElMessageBox.prompt('请输入新密码（8-32 位）', '重置密码', { inputPattern: /^\S{8,32}$/, inputErrorMessage: '密码长度必须在8-32位之间' })
  await resetUserPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function onStatusChange(row: UserItem) {
  try {
    await updateUserStatus(row.id, row.status)
    ElMessage.success('状态已更新')
  } catch (e) {
    // 后端可能拒绝（如停用自己或内置管理员），失败时回滚开关状态
    row.status = row.status === 1 ? 0 : 1
  }
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
        <el-form-item label="手机号"><el-input v-model="query.phone" clearable /></el-form-item>
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
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="deptName" label="部门" />
        <el-table-column prop="postName" label="岗位" />
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
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="form.deptId" :data="deptList" :props="{ value: 'id', label: 'name' } as any" check-strictly placeholder="选择部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色" v-permission="'system:user:assign-role'">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.postId" clearable style="width: 100%">
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
