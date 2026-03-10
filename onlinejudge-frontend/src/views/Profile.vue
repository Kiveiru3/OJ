<template>
  <div class="profile-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><User /></el-icon>
            <div class="title-stack">
              <h2 class="pro-title-text">个人中心</h2>
              <span class="title-sub">维护个人资料、角色档案与账号安全信息</span>
            </div>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">用户名</div>
          <div class="overview-value text-value">{{ userInfo.username || '-' }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">昵称</div>
          <div class="overview-value text-value">{{ userInfo.nickname || '-' }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">邮箱</div>
          <div class="overview-value text-value">{{ userInfo.email || '-' }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">角色</div>
          <div class="role-wrap">
            <el-tag :type="getRoleType(userInfo.role)" effect="dark" size="large">
              {{ getRoleText(userInfo.role) }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane label="基础信息" name="info">
          <section class="section-panel">
            <div class="section-head">
              <h3 class="section-title">资料编辑</h3>
              <span class="section-tip">更新后会同步到全站展示信息</span>
            </div>
            <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="120px" class="profile-form">
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
          </section>
        </el-tab-pane>

        <el-tab-pane label="角色档案" name="roleProfile">
          <section class="section-panel">
            <div class="section-head">
              <h3 class="section-title">角色档案</h3>
              <span class="section-tip">根据当前角色维护扩展资料（学生/教师/管理员）</span>
            </div>
            <el-skeleton :loading="roleProfileLoading" animated :rows="6">
              <el-form label-width="120px" class="profile-form">
                <el-form-item label="当前角色">
                  <el-tag :type="getRoleType(userInfo.role)" effect="dark" size="large">
                    {{ getRoleText(userInfo.role) }}
                  </el-tag>
                </el-form-item>

                <el-form-item v-if="isStudent" label="学号">
                  <el-input v-model="roleProfileForm.studentNo" placeholder="请输入学号" />
                </el-form-item>
                <el-form-item v-if="isStudent" label="班级">
                  <el-input v-model="roleProfileForm.className" placeholder="请输入班级" />
                </el-form-item>
                <el-form-item v-if="isStudent" label="专业">
                  <el-input v-model="roleProfileForm.major" placeholder="请输入专业" />
                </el-form-item>

                <el-form-item v-if="isTeacher" label="工号">
                  <el-input v-model="roleProfileForm.teacherNo" placeholder="请输入工号" />
                </el-form-item>
                <el-form-item v-if="isTeacher" label="职称">
                  <el-input v-model="roleProfileForm.title" placeholder="请输入职称" />
                </el-form-item>

                <el-form-item v-if="isAdmin" label="管理编号">
                  <el-input v-model="roleProfileForm.adminCode" placeholder="请输入管理编号" />
                </el-form-item>

                <el-form-item label="真实姓名">
                  <el-input v-model="roleProfileForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item v-if="isStudent || isTeacher" label="性别">
                  <el-input v-model="roleProfileForm.gender" placeholder="例如：男 / 女 / 保密" />
                </el-form-item>
                <el-form-item v-if="isTeacher || isAdmin" label="院系/部门">
                  <el-input v-model="roleProfileForm.department" placeholder="请输入院系或部门" />
                </el-form-item>
                <el-form-item label="个人简介">
                  <el-input
                    v-model="roleProfileForm.bio"
                    type="textarea"
                    :rows="3"
                    maxlength="500"
                    show-word-limit
                    placeholder="请输入个人简介"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="updateRoleProfile" :loading="updatingRoleProfile" size="large">
                    <el-icon><Check /></el-icon>
                    保存档案
                  </el-button>
                </el-form-item>
              </el-form>
            </el-skeleton>
          </section>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <section class="section-panel">
            <div class="section-head">
              <h3 class="section-title">安全设置</h3>
              <span class="section-tip">建议定期更换密码，避免与其他平台重复</span>
            </div>
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="120px" class="profile-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="changePassword" :loading="changing" size="large">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </section>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Check, Lock, User } from '@element-plus/icons-vue'

const userStore = useUserStore()

const activeTab = ref('info')
const updating = ref(false)
const changing = ref(false)
const roleProfileLoading = ref(false)
const updatingRoleProfile = ref(false)

const infoFormRef = ref(null)
const passwordFormRef = ref(null)

const userInfo = ref({})

const infoForm = reactive({
  nickname: '',
  email: ''
})

const roleProfileForm = reactive({
  studentNo: '',
  className: '',
  major: '',
  teacherNo: '',
  title: '',
  adminCode: '',
  department: '',
  realName: '',
  gender: '',
  bio: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isStudent = computed(() => userInfo.value?.role === 'STUDENT')
const isTeacher = computed(() => userInfo.value?.role === 'TEACHER')
const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

const validateConfirmPassword = (_, value, callback) => {
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
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
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

const resetRoleProfileForm = (data = {}) => {
  roleProfileForm.studentNo = data.studentNo || ''
  roleProfileForm.className = data.className || ''
  roleProfileForm.major = data.major || ''
  roleProfileForm.teacherNo = data.teacherNo || ''
  roleProfileForm.title = data.title || ''
  roleProfileForm.adminCode = data.adminCode || ''
  roleProfileForm.department = data.department || ''
  roleProfileForm.realName = data.realName || ''
  roleProfileForm.gender = data.gender || ''
  roleProfileForm.bio = data.bio || ''
}

const loadRoleProfile = async () => {
  roleProfileLoading.value = true
  try {
    const res = await userApi.getRoleProfile()
    resetRoleProfileForm(res.data || {})
  } catch (error) {
    ElMessage.error(error.message || '加载角色档案失败')
  } finally {
    roleProfileLoading.value = false
  }
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

const loadProfileData = async () => {
  await loadUserInfo()
  await loadRoleProfile()
}

const updateInfo = async () => {
  if (!infoFormRef.value) return
  await infoFormRef.value.validate(async (valid) => {
    if (!valid) return
    updating.value = true
    try {
      await userApi.updateUserInfo(infoForm)
      ElMessage.success('资料修改成功')
      await loadUserInfo()
    } catch (error) {
      ElMessage.error(error.message || '资料修改失败')
    } finally {
      updating.value = false
    }
  })
}

const updateRoleProfile = async () => {
  updatingRoleProfile.value = true
  try {
    await userApi.updateRoleProfile({ ...roleProfileForm })
    ElMessage.success('角色档案保存成功')
    await loadRoleProfile()
  } catch (error) {
    ElMessage.error(error.message || '角色档案保存失败')
  } finally {
    updatingRoleProfile.value = false
  }
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
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
  })
}

onMounted(() => {
  loadProfileData()
})
</script>

<style scoped>
.profile-container {
  max-width: 1100px;
  margin: 0 auto;
}

.main-card {
  border-radius: 8px;
}

.title-stack {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.title-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.overview-card {
  border: 1px solid #dce8f8;
  background: linear-gradient(160deg, #f9fbff 0%, #eef6ff 100%);
  border-radius: 8px;
  padding: 12px 14px;
  transition: transform 0.18s ease, box-shadow 0.2s ease;
}

.overview-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.1);
}

.overview-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 5px;
}

.overview-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.text-value {
  font-size: 14px;
  line-height: 1.35;
  word-break: break-all;
}

.role-wrap {
  margin-top: 2px;
}

.profile-tabs {
  margin-top: 8px;
}

.section-panel {
  border: 1px solid #e2e9f3;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 14px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.section-tip {
  font-size: 12px;
  color: #64748b;
}

.profile-form {
  max-width: 700px;
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #334155;
}

@media (max-width: 980px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
