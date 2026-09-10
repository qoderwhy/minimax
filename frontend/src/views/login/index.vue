<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCaptcha, login } from '@/api/auth'
import type { CaptchaVO } from '@/types/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaInfo = ref<CaptchaVO | null>(null)

const form = ref({
  // 仅开发环境预填演示账号，避免默认凭据随生产构建产物外泄
  username: import.meta.env.DEV ? 'admin' : '',
  password: import.meta.env.DEV ? 'admin123' : '',
  captchaId: '',
  captchaCode: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  try {
    captchaInfo.value = await getCaptcha()
    form.value.captchaId = captchaInfo.value.captchaId
    form.value.captchaCode = ''
  } catch (e) {
    ElMessage.error('验证码加载失败')
  }
}

refreshCaptcha()

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({
      username: form.value.username,
      password: form.value.password,
      captchaId: form.value.captchaId,
      captchaCode: form.value.captchaCode
    })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/')
  } catch (e: any) {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="title">qkit 后台管理系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" prefix-icon="Key" />
            <img
              v-if="captchaInfo"
              :src="captchaInfo.captchaImage"
              class="captcha-img"
              alt="验证码"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 渐变由主色派生，切换主题色时登录页同步变化 */
  background: linear-gradient(135deg, var(--el-color-primary) 0%, var(--el-color-primary-dark-2) 100%);
}
.login-box {
  width: 380px;
  padding: 32px;
  background: var(--el-bg-color);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--el-box-shadow-light);
  .title {
    text-align: center;
    margin-bottom: 24px;
    color: var(--el-text-color-primary);
  }
  .captcha-row {
    display: flex;
    gap: 8px;
    width: 100%;
    .el-input {
      flex: 1;
    }
    .captcha-img {
      width: 120px;
      height: 40px;
      cursor: pointer;
      border-radius: var(--app-radius-sm);
    }
  }
}
</style>
