<template>
  <div class="mx-auto flex min-h-screen w-full max-w-[1100px] items-center px-4 py-10">
    <div class="grid w-full gap-6 md:grid-cols-2">
      <AppCard padding="lg" class="hidden md:block">
        <AppBadge tone="success">新用户</AppBadge>
        <h1 class="mt-4 text-3xl font-bold text-slate-900">创建账号</h1>
        <p class="mt-2 prose-readable">注册后默认是学生角色，管理员可在后台将你升级为教师。</p>
      </AppCard>

      <AppCard padding="lg">
        <h2 class="text-2xl font-bold text-slate-900">注册</h2>
        <p class="mt-1 text-sm text-soft">填写基础信息即可完成注册</p>

        <form class="mt-5 space-y-4" @submit.prevent="submit">
          <div>
            <label class="mb-1 block text-sm text-slate-700">用户名</label>
            <input v-model.trim="form.username" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入用户名" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-700">昵称</label>
            <input v-model.trim="form.nickname" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入昵称" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-700">邮箱</label>
            <input v-model.trim="form.email" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入邮箱" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-700">密码</label>
            <input v-model="form.password" type="password" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入密码" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-700">确认密码</label>
            <input v-model="form.confirmPassword" type="password" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请再次输入密码" />
          </div>
          <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>
          <AppButton block :disabled="loading">{{ loading ? '提交中...' : '注册' }}</AppButton>
        </form>

        <p class="mt-4 text-sm text-soft">已有账号？<RouterLink to="/login" class="text-slate-900 underline">去登录</RouterLink></p>
      </AppCard>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import { useUserStore } from '@/stores/useUserStore'

const userStore = useUserStore()
const router = useRouter()

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const error = ref('')

const submit = async () => {
  error.value = ''
  if (!form.username || !form.email || !form.password) {
    error.value = '请填写完整信息'
    return
  }
  if (form.password.length < 6) {
    error.value = '密码至少 6 位'
    return
  }
  if (form.password !== form.confirmPassword) {
    error.value = '两次输入密码不一致'
    return
  }
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      nickname: form.nickname || form.username,
      email: form.email,
      password: form.password
    })
    router.push('/login')
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
