<template>
  <div class="profile-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><User /></el-icon>
            <h2 class="pro-title-text">个人中心</h2>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane label="个人信息" name="info">
          <div class="tab-content">
            <el-form
              ref="infoFormRef"
              :model="infoForm"
              :rules="infoRules"
              label-width="120px"
              class="profile-form"
            >
              <el-form-item label="用户名">
                <el-input v-model="userInfo.username" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="角色">
                <el-tag :type="getRoleType(userInfo.role)" effect="dark" size="large">
                  {{ getRoleText(userInfo.role) }}
                </el-tag>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updateInfo" :loading="updating" size="large">
                  <el-icon><Check /></el-icon>
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <div class="tab-content">
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="120px"
              class="profile-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入原密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="changePassword" :loading="changing" size="large">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ProfileView'
}
</script>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api'
import { ElMessage } from 'element-plus'
import { User, Check, Lock } from '@element-plus/icons-vue'

const userStore = useUserStore()

const activeTab = ref('info')
const updating = ref(false)
const changing = ref(false)
const infoFormRef = ref(null)
const passwordFormRef = ref(null)

const userInfo = ref({})

const infoForm = reactive({
  nickname: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const infoRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const getRoleType = (role) => {
  const map = {
    ADMIN: 'danger',
    TEACHER: 'warning',
    STUDENT: 'success'
  }
  return map[role] || ''
}

const getRoleText = (role) => {
  const map = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return map[role] || role
}

const loadUserInfo = async () => {
  try {
    await userStore.fetchUserInfo()
    userInfo.value = userStore.userInfo
    infoForm.nickname = userInfo.value.nickname || ''
    infoForm.email = userInfo.value.email || ''
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  }
}

const updateInfo = async () => {
  if (!infoFormRef.value) return

  await infoFormRef.value.validate(async (valid) => {
    if (valid) {
      updating.value = true
      try {
        await userApi.updateUserInfo(infoForm)
        ElMessage.success('修改成功')
        await loadUserInfo()
      } catch (error) {
        ElMessage.error(error.message || '修改失败')
      } finally {
        updating.value = false
      }
    }
  })
}

const changePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      changing.value = true
      try {
        await userApi.changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功，请重新登录')
        passwordFormRef.value.resetFields()
        setTimeout(() => {
          userStore.logout()
          window.location.href = '/login'
        }, 1500)
      } catch (error) {
        ElMessage.error(error.message || '密码修改失败')
      } finally {
        changing.value = false
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  max-width: 900px;
  margin: 0 auto;
}

.main-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 20px;
  color: #1677ff;
}

.card-header h2 {
  margin: 0;
}

.profile-tabs {
  margin-top: 20px;
}

.tab-content {
  padding: 20px;
}

.profile-form {
  max-width: 600px;
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}
</style>
