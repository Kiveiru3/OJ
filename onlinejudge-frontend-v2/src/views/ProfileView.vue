<template>
  <section class="space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="section-title">个人设置</h1>
        <p class="section-subtitle">维护公开资料、角色档案和账户安全，修改后会同步到个人主页与社区展示。</p>
      </div>

      <div class="flex flex-wrap gap-2 text-xs">
        <span class="rounded-full border border-line bg-white px-3 py-1.5 text-slate-700">{{ roleText }}</span>
        <span class="rounded-full border border-line bg-white px-3 py-1.5 text-slate-700">@{{ userInfo.username || 'username' }}</span>
        <span class="rounded-full border border-line bg-white px-3 py-1.5 text-slate-700">{{ accountStatusText }}</span>
      </div>
    </header>

    <AppCard padding="lg">
      <div class="grid gap-5 lg:grid-cols-[96px_minmax(0,1fr)_220px] lg:items-center">
        <UserAvatar :user="{ ...userInfo, avatar: form.avatar || userInfo.avatar }" size="xl" />

        <div class="min-w-0">
          <div class="text-xl font-semibold text-slate-900">{{ previewName }}</div>
          <div class="mt-1 text-sm text-soft break-all">
            {{ form.email || userInfo.email || '未设置邮箱' }}
          </div>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
            这里主要处理会影响公开展示和账号使用的关键信息，尽量保持准确、简洁即可。
          </p>
        </div>

        <dl class="grid gap-3 text-sm lg:border-l lg:border-line lg:pl-5">
          <div class="flex items-center justify-between gap-3">
            <dt class="text-soft">用户名</dt>
            <dd class="font-medium text-slate-900">{{ userInfo.username || '-' }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-soft">当前身份</dt>
            <dd class="font-medium text-slate-900">{{ roleText }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-soft">账号状态</dt>
            <dd class="font-medium text-slate-900">{{ accountStatusText }}</dd>
          </div>
        </dl>
      </div>
    </AppCard>

    <div class="grid gap-4 xl:grid-cols-[1fr_1fr]">
      <AppCard padding="lg" class="space-y-5">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-slate-800">基础资料</h2>
            <p class="mt-1 text-xs text-soft">头像、昵称和邮箱会直接影响主页与社区里的公开展示。</p>
          </div>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">公开显示</span>
        </div>

        <div class="grid gap-5 lg:grid-cols-[180px_minmax(0,1fr)]">
          <div class="space-y-3">
            <div class="rounded-xl border border-line bg-slate-50 p-4 text-center">
              <UserAvatar
                :user="{ ...userInfo, nickname: form.nickname || userInfo.nickname, avatar: form.avatar || userInfo.avatar }"
                size="xl"
              />
              <div class="mt-3 text-sm font-semibold text-slate-900">{{ previewName }}</div>
              <div class="mt-1 text-xs text-soft">@{{ userInfo.username || '-' }}</div>
            </div>

            <div class="flex flex-wrap gap-2">
              <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleAvatarFile"
              />
              <AppButton size="sm" variant="secondary" @click="pickAvatarFile">上传头像</AppButton>
              <AppButton size="sm" variant="ghost" @click="resetAvatar">恢复默认</AppButton>
            </div>

            <p class="text-xs leading-6 text-soft">建议使用 200 x 200 以内图片，体积不超过 1MB。</p>
          </div>

          <div class="grid gap-4">
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">昵称</span>
              <input
                v-model.trim="form.nickname"
                class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
                placeholder="请输入昵称"
              />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">邮箱</span>
              <input
                v-model.trim="form.email"
                class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
                placeholder="请输入邮箱"
              />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">头像链接</span>
              <input
                v-model.trim="form.avatar"
                class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
                placeholder="支持 http / https / data:image"
              />
            </label>
          </div>
        </div>

        <p v-if="infoError" class="text-sm text-rose-600">{{ infoError }}</p>
        <div class="flex justify-end">
          <AppButton :disabled="savingInfo" @click="saveInfo">{{ savingInfo ? '保存中...' : '保存基础资料' }}</AppButton>
        </div>
      </AppCard>

      <AppCard padding="lg" class="space-y-5">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-slate-800">角色档案</h2>
            <p class="mt-1 text-xs text-soft">{{ roleProfileHint }}</p>
          </div>
          <span class="rounded-full border border-line bg-white px-3 py-1 text-xs font-medium text-slate-600">{{ roleText }}</span>
        </div>

        <div class="grid gap-3 md:grid-cols-2">
          <label v-if="isStudent" class="space-y-2">
            <span class="text-sm font-medium text-slate-700">学号</span>
            <input v-model.trim="roleForm.studentNo" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入学号" />
          </label>
          <label v-if="isStudent" class="space-y-2">
            <span class="text-sm font-medium text-slate-700">班级</span>
            <input v-model.trim="roleForm.className" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入班级" />
          </label>
          <label v-if="isStudent" class="space-y-2">
            <span class="text-sm font-medium text-slate-700">专业</span>
            <input v-model.trim="roleForm.major" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入专业" />
          </label>

          <label v-if="isTeacher" class="space-y-2">
            <span class="text-sm font-medium text-slate-700">工号</span>
            <input v-model.trim="roleForm.teacherNo" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入工号" />
          </label>
          <label v-if="isTeacher" class="space-y-2">
            <span class="text-sm font-medium text-slate-700">职称</span>
            <input v-model.trim="roleForm.title" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入职称" />
          </label>

          <label v-if="isAdmin" class="space-y-2">
            <span class="text-sm font-medium text-slate-700">管理编号</span>
            <input v-model.trim="roleForm.adminCode" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入管理编号" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">真实姓名</span>
            <input v-model.trim="roleForm.realName" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入真实姓名" />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">性别</span>
            <input v-model.trim="roleForm.gender" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="可选" />
          </label>
          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">院系 / 部门</span>
            <input v-model.trim="roleForm.department" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入院系或部门" />
          </label>
          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">个人简介</span>
            <textarea v-model.trim="roleForm.bio" class="h-28 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="用一两句话介绍自己" />
          </label>
        </div>

        <p v-if="roleError" class="text-sm text-rose-600">{{ roleError }}</p>
        <div class="flex justify-end">
          <AppButton :disabled="savingRole" @click="saveRole">{{ savingRole ? '保存中...' : '保存角色档案' }}</AppButton>
        </div>
      </AppCard>
    </div>

    <AppCard padding="lg" class="space-y-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold text-slate-800">账户安全</h2>
          <p class="mt-1 text-xs text-soft">修改密码后会自动退出登录，请使用新密码重新进入系统。</p>
        </div>
        <span class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700">建议定期更换密码</span>
      </div>

      <div class="grid gap-3 md:grid-cols-3">
        <label class="space-y-2">
          <span class="text-sm font-medium text-slate-700">当前密码</span>
          <input v-model="pwd.oldPassword" type="password" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入当前密码" />
        </label>
        <label class="space-y-2">
          <span class="text-sm font-medium text-slate-700">新密码</span>
          <input v-model="pwd.newPassword" type="password" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="至少 6 位" />
        </label>
        <label class="space-y-2">
          <span class="text-sm font-medium text-slate-700">确认新密码</span>
          <input v-model="pwd.confirmPassword" type="password" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="再次输入新密码" />
        </label>
      </div>

      <p v-if="pwdError" class="text-sm text-rose-600">{{ pwdError }}</p>
      <div class="flex justify-end">
        <AppButton :disabled="savingPwd" @click="savePassword">{{ savingPwd ? '提交中...' : '更新密码' }}</AppButton>
      </div>
    </AppCard>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useUserStore } from '@/stores/useUserStore'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref({})

const form = reactive({ nickname: '', email: '', avatar: '' })
const roleForm = reactive({
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

const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const savingInfo = ref(false)
const savingRole = ref(false)
const savingPwd = ref(false)

const infoError = ref('')
const roleError = ref('')
const pwdError = ref('')
const fileInputRef = ref(null)

const roleMap = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }

const roleText = computed(() => roleMap[userInfo.value?.role] || userInfo.value?.role || '-')
const isStudent = computed(() => userInfo.value?.role === 'STUDENT')
const isTeacher = computed(() => userInfo.value?.role === 'TEACHER')
const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
const previewName = computed(() => form.nickname || userInfo.value?.nickname || userInfo.value?.username || '个人中心')
const accountStatusText = computed(() => (userInfo.value?.status === 0 ? '账号已禁用' : '账号正常'))
const roleProfileHint = computed(() => {
  if (isStudent.value) return '补全学号、班级和专业后，教学场景里会更容易识别你的身份。'
  if (isTeacher.value) return '补全工号、职称和院系信息后，课程与讨论区展示会更完整。'
  if (isAdmin.value) return '维护管理编号和基础资料，方便在后台审计与协作中快速识别。'
  return '根据当前身份维护专属档案信息。'
})

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
  form.avatar = userInfo.value.avatar || ''
  applyRoleData(rRes.data || {})
  userStore.userInfo = userInfo.value
  localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
}

function pickAvatarFile() {
  fileInputRef.value?.click()
}

function resetAvatar() {
  form.avatar = ''
}

function handleAvatarFile(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    infoError.value = '请选择图片文件'
    return
  }
  if (file.size > 1024 * 1024) {
    infoError.value = '图片不能超过 1MB'
    return
  }
  infoError.value = ''
  const reader = new FileReader()
  reader.onload = () => {
    form.avatar = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

async function saveInfo() {
  infoError.value = ''
  if (!form.nickname || !form.email) {
    infoError.value = '昵称和邮箱不能为空'
    return
  }
  savingInfo.value = true
  try {
    await userApi.updateUserInfo({
      nickname: form.nickname,
      email: form.email,
      avatar: form.avatar
    })
    await loadProfile()
  } catch (e) {
    infoError.value = e.message || '保存失败'
  } finally {
    savingInfo.value = false
  }
}

async function saveRole() {
  roleError.value = ''
  savingRole.value = true
  try {
    await userApi.updateRoleProfile({ ...roleForm })
    await loadProfile()
  } catch (e) {
    roleError.value = e.message || '保存角色档案失败'
  } finally {
    savingRole.value = false
  }
}

async function savePassword() {
  pwdError.value = ''
  if (!pwd.oldPassword || !pwd.newPassword) {
    pwdError.value = '请填写当前密码和新密码'
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
