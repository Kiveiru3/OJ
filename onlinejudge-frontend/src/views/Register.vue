<template>
  <div class="auth-page register-page">
    <div class="auth-background" />
    <div class="auth-content">
      <section class="brand-panel">
        <div class="brand-chip">新用户注册</div>
        <h1>创建你的 OJ 学习账号</h1>
        <p>注册后可参与题库训练、竞赛活动和讨论区交流，系统将持续记录你的成长轨迹。</p>
        <ul class="brand-points">
          <li>支持按难度规划刷题路径</li>
          <li>实时查看提交结果与评测数据</li>
          <li>参与课程竞赛并进入排行榜</li>
        </ul>
      </section>

      <el-card class="auth-card card-shadow">
        <template #header>
          <div class="card-header">
            <div class="logo-wrap">
              <el-icon class="logo-icon"><Trophy /></el-icon>
            </div>
            <div class="header-text">
              <h2>账号注册</h2>
              <p>填写下列信息，完成账号创建</p>
            </div>
          </div>
        </template>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="registerForm.nickname"
              placeholder="请输入昵称"
              size="large"
              :prefix-icon="UserFilled"
              clearable
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              size="large"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item class="submit-row">
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="submit-btn"
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '注册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="switch-link">
          <span>已有账号？</span>
          <el-link type="primary" :underline="false" @click="$router.push('/login')">
            立即登录
          </el-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RegisterView'
}
</script>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Trophy, User, UserFilled, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.register({
        username: registerForm.username,
        nickname: registerForm.nickname,
        email: registerForm.email,
        password: registerForm.password
      })
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      ElMessage.error(error.message || '注册失败')
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
    radial-gradient(circle at 10% 8%, rgba(100, 116, 139, 0.16) 0, rgba(100, 116, 139, 0) 34%),
    radial-gradient(circle at 92% 82%, rgba(30, 64, 175, 0.12) 0, rgba(30, 64, 175, 0) 40%),
    linear-gradient(148deg, #f5f7fb 0%, #eceff5 48%, #f7f9fd 100%);
}

.auth-content {
  position: relative;
  z-index: 1;
  max-width: 1140px;
  margin: 0 auto;
  min-height: calc(100vh - 56px);
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 450px);
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
  font-size: 34px;
  letter-spacing: 0.3px;
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
  background: linear-gradient(145deg, #2563eb 0%, #0ea5e9 100%);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.14);
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
  margin-top: 8px;
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
