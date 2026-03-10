<template>
  <div class="auth-page login-page">
    <div class="auth-background" />
    <div class="auth-content">
      <section class="brand-panel">
        <div class="brand-chip">在线评测平台</div>
        <h1>程序设计教学平台</h1>
        <p>在线做题、自动评测、竞赛训练与讨论交流，一站式完成课程实践闭环。</p>
        <ul class="brand-points">
          <li>题库练习与评测记录可追踪</li>
          <li>竞赛组织、榜单与成绩分析</li>
          <li>教师与管理员协同管理</li>
        </ul>
      </section>

      <el-card class="auth-card card-shadow">
        <template #header>
          <div class="card-header">
            <div class="logo-wrap">
              <el-icon class="logo-icon"><Trophy /></el-icon>
            </div>
            <div class="header-text">
              <h2>账号登录</h2>
              <p>欢迎回来，请输入账号信息</p>
            </div>
          </div>
        </template>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item class="submit-row">
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="submit-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="switch-link">
          <span>还没有账号？</span>
          <el-link type="primary" :underline="false" @click="$router.push('/register')">
            立即注册
          </el-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginView'
}
</script>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Trophy, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(loginForm)
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/problems'
      router.push(redirect)
    } catch (error) {
      ElMessage.error(error.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  padding: 28px;
  overflow: hidden;
}

.auth-background {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 8% 12%, rgba(15, 23, 42, 0.08) 0, rgba(15, 23, 42, 0) 34%),
    radial-gradient(circle at 92% 88%, rgba(59, 130, 246, 0.12) 0, rgba(59, 130, 246, 0) 38%),
    linear-gradient(145deg, #f5f7fb 0%, #edf1f7 45%, #f8fafd 100%);
}

.auth-content {
  position: relative;
  z-index: 1;
  max-width: 1140px;
  margin: 0 auto;
  min-height: calc(100vh - 56px);
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 430px);
  align-items: center;
  gap: 38px;
}

.brand-panel {
  padding: 12px 8px;
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #d6dfec;
  background: rgba(255, 255, 255, 0.75);
  color: #425466;
  font-size: 12px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.brand-panel h1 {
  margin-top: 16px;
  font-size: 36px;
  letter-spacing: 0.4px;
  color: #0f172a;
}

.brand-panel p {
  margin-top: 14px;
  max-width: 560px;
  color: #475569;
  line-height: 1.8;
  font-size: 15px;
}

.brand-points {
  margin-top: 20px;
  list-style: none;
  padding: 0;
  display: grid;
  gap: 10px;
}

.brand-points li {
  position: relative;
  padding-left: 18px;
  color: #334155;
  font-size: 14px;
}

.brand-points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: linear-gradient(145deg, #3b82f6 0%, #0ea5e9 100%);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.14);
}

.auth-card {
  border-radius: 8px;
  border: 1px solid #d7e1ee;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97) 0%, #ffffff 100%);
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.12);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-wrap {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2463d8;
  background: linear-gradient(140deg, #e8f0fd 0%, #dceafe 100%);
  border: 1px solid #c8daf3;
}

.logo-icon {
  font-size: 24px;
}

.header-text h2 {
  margin: 0;
  font-size: 23px;
  color: #111827;
}

.header-text p {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.auth-form {
  margin-top: 10px;
}

.auth-form :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 600;
  padding-bottom: 8px;
}

.submit-row {
  margin-top: 10px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 15px;
  transition: transform 0.18s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
}

.submit-btn:active {
  transform: translateY(0) scale(0.99);
  filter: brightness(0.98);
}

.switch-link {
  margin-top: 20px;
  text-align: center;
  color: #64748b;
  font-size: 14px;
}

.switch-link .el-link {
  margin-left: 4px;
  font-weight: 600;
}

@media (max-width: 960px) {
  .auth-page {
    padding: 16px;
  }

  .auth-content {
    min-height: calc(100vh - 32px);
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .brand-panel h1 {
    font-size: 28px;
  }

  .brand-panel p {
    font-size: 14px;
  }
}
</style>
