<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getProfile, updateProfile, changePassword, type ProfileVO, type ProfileUpdate } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import { required, mobile, email } from '@/utils/validate'
import DictSelect from '@/components/DictSelect.vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'info' | 'password'>('info')
const profileLoading = ref(false)
const infoSaving = ref(false)
const pwdSaving = ref(false)

const profile = ref<ProfileVO | null>(null)

const infoForm = reactive<ProfileUpdate>({
  nickname: '',
  realName: '',
  email: '',
  phone: '',
  avatar: '',
  sex: 0,
  remark: ''
})
const infoFormRef = ref<FormInstance>()

const infoRules: FormRules = {
  nickname: [required('请输入昵称')],
  email: [email()],
  phone: [mobile()]
}

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdFormRef = ref<FormInstance>()

const pwdRules: FormRules = {
  oldPassword: [required('请输入原密码')],
  newPassword: [
    required('请输入新密码'),
    { min: 8, max: 32, message: '密码长度必须在8-32位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    required('请确认新密码'),
    {
      validator: (_r, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const displayName = computed(() => profile.value?.nickname || profile.value?.username || '用户')

async function loadProfile() {
  profileLoading.value = true
  try {
    const data = await getProfile()
    profile.value = data
    infoForm.nickname = data.nickname ?? ''
    infoForm.realName = data.realName ?? ''
    infoForm.email = data.email ?? ''
    infoForm.phone = data.phone ?? ''
    infoForm.avatar = data.avatar ?? ''
    infoForm.sex = data.sex ?? 0
    infoForm.remark = data.remark ?? ''
  } finally {
    profileLoading.value = false
  }
}

async function onSaveInfo() {
  if (!infoFormRef.value) return
  const valid = await infoFormRef.value.validate().catch(() => false)
  if (!valid) return
  infoSaving.value = true
  try {
    await updateProfile({
      ...infoForm,
      // 性别字典值为字符串，统一转为数字以对齐后端 Integer
      sex: infoForm.sex == null ? undefined : Number(infoForm.sex)
    })
    ElMessage.success('资料更新成功')
    // 同步刷新顶部导航展示的昵称/头像
    await userStore.fetchUserInfo()
    await loadProfile()
  } finally {
    infoSaving.value = false
  }
}

async function onChangePassword() {
  if (!pwdFormRef.value) return
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdSaving.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout(true)
    router.push('/login')
  } finally {
    pwdSaving.value = false
  }
}

function onResetInfo() {
  infoFormRef.value?.clearValidate()
  loadProfile()
}

function onResetPwd() {
  pwdFormRef.value?.resetFields()
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page" v-loading="profileLoading">
    <el-row :gutter="16">
      <!-- 左侧：用户卡片 -->
      <el-col :span="8">
        <el-card class="user-card">
          <div class="user-head">
            <el-avatar :size="96" :src="profile?.avatar || ''" />
            <h3 class="name">{{ displayName }}</h3>
            <p class="role">{{ profile?.deptName || '—' }}</p>
          </div>
          <el-divider />
          <ul class="user-meta">
            <li><span class="label">用户名</span><span>{{ profile?.username || '—' }}</span></li>
            <li><span class="label">真实姓名</span><span>{{ profile?.realName || '—' }}</span></li>
            <li><span class="label">手机号</span><span>{{ profile?.phone || '—' }}</span></li>
            <li><span class="label">邮箱</span><span>{{ profile?.email || '—' }}</span></li>
            <li><span class="label">岗位</span><span>{{ profile?.postName || '—' }}</span></li>
            <li><span class="label">最近登录</span><span>{{ profile?.loginDate || '—' }}</span></li>
            <li><span class="label">登录IP</span><span>{{ profile?.loginIp || '—' }}</span></li>
          </ul>
        </el-card>
      </el-col>

      <!-- 右侧：资料编辑 / 修改密码 -->
      <el-col :span="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="info">
              <el-form
                ref="infoFormRef"
                :model="infoForm"
                :rules="infoRules"
                label-width="100px"
                style="max-width: 560px"
              >
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="infoForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="性别" prop="sex">
                  <dict-select
                    dict-type="sys_user_sex"
                    :model-value="infoForm.sex == null ? undefined : String(infoForm.sex)"
                    @update:model-value="(v) => (infoForm.sex = v == null ? undefined : Number(v))"
                  />
                </el-form-item>
                <el-form-item label="头像地址" prop="avatar">
                  <el-input v-model="infoForm.avatar" placeholder="请输入头像图片地址">
                    <template #prepend>
                      <el-avatar :size="28" :src="infoForm.avatar || ''" />
                    </template>
                  </el-input>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="infoForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="infoSaving" @click="onSaveInfo">保存</el-button>
                  <el-button @click="onResetInfo">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="100px"
                style="max-width: 480px"
              >
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-32位" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="pwdSaving" @click="onChangePassword">提交</el-button>
                  <el-button @click="onResetPwd">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  padding: 4px;
}
.user-card {
  .user-head {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    .name {
      margin: 4px 0 0;
      font-size: 18px;
    }
    .role {
      margin: 0;
      color: #909399;
      font-size: 13px;
    }
  }
  .user-meta {
    list-style: none;
    margin: 0;
    padding: 0;
    li {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      font-size: 14px;
      .label {
        color: #909399;
      }
    }
  }
}
</style>
