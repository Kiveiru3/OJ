<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">个人中心</h1>
      <p class="section-subtitle">维护账号资料、角色档案与密码安全。</p>
    </header>

    <div class="grid gap-4 md:grid-cols-[0.7fr_1.3fr]">
      <AppCard>
        <div class="flex items-center gap-3">
          <div class="flex h-14 w-14 items-center justify-center rounded-full bg-slate-900 text-lg font-semibold text-white">{{ avatarText }}</div>
          <div>
            <div class="text-lg font-semibold text-slate-800">{{ userInfo.nickname || userInfo.username || '-' }}</div>
            <div class="text-xs text-soft">{{ roleText }}</div>
          </div>
        </div>
        <div class="mt-4 grid gap-2 text-sm text-slate-600">
          <div>用户名：{{ userInfo.username || '-' }}</div>
          <div>邮箱：{{ userInfo.email || '-' }}</div>
          <div>状态：{{ userInfo.status === 0 ? '禁用' : '启用' }}</div>
        </div>
      </AppCard>

      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">基础资料</h2>
        <div class="mt-3 grid gap-3">
          <input v-model.trim="form.nickname" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="昵称" />
          <input v-model.trim="form.email" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="邮箱" />
          <p v-if="infoError" class="text-sm text-rose-600">{{ infoError }}</p>
          <AppButton :disabled="savingInfo" @click="saveInfo">{{ savingInfo ? '保存中...' : '保存资料' }}</AppButton>
        </div>
      </AppCard>
    </div>

    <div class="grid gap-4 md:grid-cols-2">
      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">角色档案</h2>
        <div class="mt-3 grid gap-3">
          <input v-if="isStudent" v-model.trim="roleForm.studentNo" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="学号" />
          <input v-if="isStudent" v-model.trim="roleForm.className" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="班级" />
          <input v-if="isStudent" v-model.trim="roleForm.major" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="专业" />

          <input v-if="isTeacher" v-model.trim="roleForm.teacherNo" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="工号" />
          <input v-if="isTeacher" v-model.trim="roleForm.title" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="职称" />

          <input v-if="isAdmin" v-model.trim="roleForm.adminCode" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="管理编号" />

          <input v-model.trim="roleForm.realName" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="真实姓名" />
          <input v-model.trim="roleForm.gender" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="性别（可选）" />
          <input v-model.trim="roleForm.department" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="院系/部门（可选）" />
          <textarea v-model.trim="roleForm.bio" class="h-24 rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="个人简介" />

          <AppButton :disabled="savingRole" @click="saveRole">{{ savingRole ? '保存中...' : '保存档案' }}</AppButton>
        </div>
      </AppCard>

      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">修改密码</h2>
        <div class="mt-3 grid gap-3">
          <input v-model="pwd.oldPassword" type="password" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="旧密码" />
          <input v-model="pwd.newPassword" type="password" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="新密码（至少6位）" />
          <input v-model="pwd.confirmPassword" type="password" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="确认新密码" />
          <p v-if="pwdError" class="text-sm text-rose-600">{{ pwdError }}</p>
          <AppButton :disabled="savingPwd" @click="savePassword">{{ savingPwd ? '提交中...' : '更新密码' }}</AppButton>
        </div>
      </AppCard>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import { useUserStore } from '@/stores/useUserStore'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref({})

const form = reactive({ nickname: '', email: '' })
const roleForm = reactive({
  studentNo: '', className: '', major: '', teacherNo: '', title: '', adminCode: '', department: '', realName: '', gender: '', bio: ''
})

const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const savingInfo = ref(false)
const savingRole = ref(false)
const savingPwd = ref(false)

const infoError = ref('')
const pwdError = ref('')

const roleMap = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }

const roleText = computed(() => roleMap[userInfo.value?.role] || userInfo.value?.role || '-')
const avatarText = computed(() => (userInfo.value?.nickname || userInfo.value?.username || 'U').slice(0, 1))
const isStudent = computed(() => userInfo.value?.role === 'STUDENT')
const isTeacher = computed(() => userInfo.value?.role === 'TEACHER')
const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

function applyRoleData(data = {}) {
  roleForm.studentNo = data.studentNo || ''
  roleForm.className = data.className || ''
  roleForm.major = data.major || ''
  roleForm.teacherNo = data.teacherNo || ''
  roleForm.title = data.title || ''
  roleForm.adminCode = data.adminCode || ''
  roleForm.department = data.department || ''
  roleForm.realName = data.realName || ''
  roleForm.gender = data.gender || ''
  roleForm.bio = data.bio || ''
}

async function loadProfile() {
  const [uRes, rRes] = await Promise.all([userApi.getUserInfo(), userApi.getRoleProfile()])
  userInfo.value = uRes.data || {}
  form.nickname = userInfo.value.nickname || ''
  form.email = userInfo.value.email || ''
  applyRoleData(rRes.data || {})
  userStore.userInfo = userInfo.value
  localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
}

async function saveInfo() {
  infoError.value = ''
  if (!form.nickname || !form.email) {
    infoError.value = '昵称和邮箱不能为空'
    return
  }
  savingInfo.value = true
  try {
    await userApi.updateUserInfo({ nickname: form.nickname, email: form.email })
    await loadProfile()
  } catch (e) {
    infoError.value = e.message || '保存失败'
  } finally {
    savingInfo.value = false
  }
}

async function saveRole() {
  savingRole.value = true
  try {
    await userApi.updateRoleProfile({ ...roleForm })
    await loadProfile()
  } finally {
    savingRole.value = false
  }
}

async function savePassword() {
  pwdError.value = ''
  if (!pwd.oldPassword || !pwd.newPassword) {
    pwdError.value = '请填写旧密码和新密码'
    return
  }
  if (pwd.newPassword.length < 6) {
    pwdError.value = '新密码长度至少 6 位'
    return
  }
  if (pwd.newPassword !== pwd.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  savingPwd.value = true
  try {
    await userApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    await userStore.logout()
    router.push('/login')
  } catch (e) {
    pwdError.value = e.message || '修改密码失败'
  } finally {
    savingPwd.value = false
  }
}

onMounted(loadProfile)
</script>
