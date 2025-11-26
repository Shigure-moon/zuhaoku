<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="title">登录</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="80px">
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="loginForm.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer">
        <span>还没有账号？</span>
        <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  mobile: '',
  password: '',
})

const rules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// 根据用户角色获取默认跳转路径
const getDefaultRedirect = () => {
  const role = userStore.userInfo?.role?.toUpperCase()
  if (role === 'TENANT') {
    return '/tenant/account-list'
  } else if (role === 'OWNER') {
    return '/owner/accounts'
  } else if (role === 'OPERATOR') {
    return '/admin/dashboard'
  }
  return '/home'
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.userLogin(loginForm.mobile, loginForm.password)
        ElMessage.success('登录成功')
        
        // 根据用户角色自动跳转到对应工作台
        const redirect = (route.query.redirect as string) || getDefaultRedirect()
        try {
          await router.push(redirect)
        } catch (navError) {
          console.error('导航失败:', navError)
          // 如果导航失败，尝试跳转到首页
          router.push('/home').catch(() => {
            console.error('跳转首页也失败')
          })
        }
      } catch (error: any) {
        console.error('登录失败:', error)
        ElMessage.error(error?.message || '登录失败，请重试')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-box {
    width: 400px;
    padding: 40px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

    .title {
      text-align: center;
      margin-bottom: 30px;
      color: #333;
    }

    .footer {
      text-align: center;
      margin-top: 20px;
      color: #666;
    }
  }
}
</style>

