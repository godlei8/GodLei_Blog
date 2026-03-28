<template>
  <div class="login-container">
    <div class="login-box page-card">
      <h2>后台登录</h2>
      <el-form @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="用户名">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-button class="submit-btn" type="primary" :loading="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </el-form>
      <p class="login-tip">默认账号：admin，默认密码：admin</p>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginApi } from '@/api'

export default {
  name: 'LoginPage',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(false)
    const errorMsg = ref('')
    const loginForm = ref({
      username: '',
      password: ''
    })

    onMounted(() => {
      if (route.query.reason === 'expired') {
        errorMsg.value = '登录已过期，请重新登录'
      }
    })

    const handleLogin = async () => {
      const username = loginForm.value.username.trim()
      const password = loginForm.value.password

      if (!username || !password) {
        errorMsg.value = '请输入用户名和密码'
        return
      }

      loading.value = true
      errorMsg.value = ''

      try {
        const res = await loginApi({
          username,
          password
        })

        if (!res?.token) {
          throw new Error('登录成功，但接口没有返回 token')
        }

        localStorage.setItem('token', res.token)
        router.replace('/')
      } catch (error) {
        errorMsg.value = error.message || '登录失败，请检查用户名和密码'
      } finally {
        loading.value = false
      }
    }

    return {
      loading,
      errorMsg,
      loginForm,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #eef4ff 0%, #f7f9fc 100%);
}

.login-box {
  width: 420px;
  padding: 28px;
}

h2 {
  margin: 0 0 18px;
  font-size: 22px;
  color: #111827;
}

.submit-btn {
  width: 100%;
}

.login-tip {
  margin: 14px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.error-msg {
  color: #f1b0a4;
  margin: 12px 0 0;
  text-align: left;
  font-size: 13px;
}

@media (max-width: 768px) {
  .login-container {
    padding: 14px;
  }

  .login-box {
    width: 100%;
    padding: 18px;
  }
}
</style>
