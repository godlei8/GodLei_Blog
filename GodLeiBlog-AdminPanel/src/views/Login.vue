<template>
  <div class="login-shell" :style="{ '--login-backdrop': `url(${loginBackdrop})` }">
    <div class="login-shell__backdrop"></div>

    <section class="login-hero">
      <span class="login-hero__eyebrow">GodLeiBlog Admin</span>
      <h1>GodLeiBlog Admin</h1>
      <p class="login-hero__lead">统一管理文章、动态、站点配置与前台体验。</p>

      <div class="login-hero__chips">
        <span>Content</span>
        <span>Moments</span>
        <span>Settings</span>
      </div>
    </section>

    <section class="login-panel page-card">
      <span class="login-panel__eyebrow">后台管理</span>
      <h2>欢迎回来</h2>
      <p class="login-panel__subtitle">登录后继续维护你的内容、视觉与站点节奏。</p>

      <el-form class="login-form" @submit.prevent="handleLogin" label-position="top">
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

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginApi } from '@/api'
import loginBackdrop from '@/assets/login-admin-bg-v2.jpg'

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
    errorMsg.value = '登录已过期，请重新登录。'
  }
})

async function handleLogin() {
  const username = loginForm.value.username.trim()
  const password = loginForm.value.password

  if (!username || !password) {
    errorMsg.value = '请输入用户名和密码。'
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
      throw new Error('登录成功，但接口没有返回 token。')
    }

    localStorage.setItem('token', res.token)
    router.replace('/')
  } catch (error) {
    errorMsg.value = error.message || '登录失败，请检查用户名和密码。'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 1.18fr) minmax(380px, 440px);
  align-items: center;
  gap: clamp(18px, 3vw, 52px);
  padding: clamp(24px, 4vw, 48px);
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(20, 5, 10, 0.88), rgba(38, 8, 18, 0.72)),
    radial-gradient(circle at 18% 18%, rgba(214, 173, 92, 0.16), transparent 24%),
    #16070c;
}

.login-shell__backdrop {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(13, 4, 7, 0.7) 0%, rgba(13, 4, 7, 0.52) 34%, rgba(13, 4, 7, 0.78) 100%),
    var(--login-backdrop) center 42% / cover no-repeat;
  transform: scale(1.06);
  filter: saturate(0.84) brightness(0.7) contrast(1.04);
}

.login-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 20%, rgba(214, 173, 92, 0.16), transparent 0 20%),
    radial-gradient(circle at 72% 16%, rgba(122, 29, 45, 0.16), transparent 0 18%),
    linear-gradient(180deg, rgba(8, 2, 4, 0.08), rgba(8, 2, 4, 0.34));
  pointer-events: none;
}

.login-hero,
.login-panel {
  position: relative;
  z-index: 1;
}

.login-hero {
  display: grid;
  gap: 18px;
  align-content: center;
  max-width: 660px;
  color: #fff5e8;
}

.login-hero__eyebrow,
.login-panel__eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(214, 173, 92, 0.28);
  background: rgba(255, 247, 234, 0.08);
  color: rgba(255, 240, 220, 0.86);
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.login-panel__eyebrow {
  border-color: rgba(214, 173, 92, 0.34);
  background: rgba(214, 173, 92, 0.12);
  color: #9b6a1f;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.48);
}

.login-hero h1 {
  margin: 0;
  font-size: clamp(3rem, 6.2vw, 5.6rem);
  line-height: 0.92;
  letter-spacing: -0.04em;
  text-wrap: balance;
}

.login-hero__lead {
  margin: 0;
  max-width: 560px;
  color: rgba(255, 241, 223, 0.78);
  font-size: clamp(1rem, 1rem + 0.2vw, 1.18rem);
  line-height: 1.8;
}

.login-hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.login-hero__chips span {
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(255, 247, 234, 0.08);
  border: 1px solid rgba(214, 173, 92, 0.18);
  color: rgba(255, 240, 220, 0.84);
  font-size: 13px;
}

.login-panel {
  width: min(440px, 100%);
  justify-self: start;
  padding: 28px;
  border-radius: 30px;
  border: 1px solid rgba(214, 173, 92, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(248, 239, 227, 0.9));
  box-shadow:
    0 28px 60px rgba(11, 2, 5, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(18px);
  transform: translateX(-30px);
}

.login-panel h2 {
  margin: 16px 0 8px;
  font-size: 32px;
  color: #2c180f;
}

.login-panel__subtitle {
  margin: 0 0 22px;
  color: #7d6651;
  line-height: 1.7;
}

.login-form {
  display: grid;
  gap: 4px;
}

.submit-btn {
  width: 100%;
  min-height: 46px;
  margin-top: 6px;
  border: none;
  background: linear-gradient(135deg, #8b2d3e, #d6ad5c);
  box-shadow: 0 14px 30px rgba(122, 29, 45, 0.2);
}

.error-msg {
  margin: 16px 0 0;
  color: #b54040;
  font-size: 13px;
  line-height: 1.6;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__label) {
  color: #503628;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px rgba(214, 173, 92, 0.14) inset;
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 24px;
    padding: 22px;
  }

  .login-hero {
    max-width: none;
  }

  .login-panel {
    justify-self: stretch;
    transform: none;
  }
}

@media (max-width: 640px) {
  .login-shell {
    padding: 16px;
  }

  .login-hero {
    gap: 14px;
  }

  .login-panel {
    padding: 20px;
    border-radius: 24px;
  }

  .login-panel h2 {
    font-size: 28px;
  }
}
</style>
