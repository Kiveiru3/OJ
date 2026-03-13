<template>
  <div class="mx-auto flex min-h-screen w-full max-w-[1100px] items-center px-4 py-10">
    <div class="grid w-full gap-6 md:grid-cols-2">
      <AppCard padding="lg" class="hidden md:block">
        <AppBadge tone="info">OJ Nova</AppBadge>
        <h1 class="mt-4 text-3xl font-bold text-slate-900">欢迎回来</h1>
        <p class="mt-2 prose-readable">登录后即可继续做题、查看提交记录、参与竞赛与讨论。</p>
      </AppCard>

      <AppCard padding="lg">
        <h2 class="text-2xl font-bold text-slate-900">登录</h2>
        <p class="mt-1 text-sm text-soft">请输入账号与密码</p>

        <form class="mt-5 space-y-4" @submit.prevent="submit">
          <div>
            <label class="mb-1 block text-sm text-slate-700">用户名</label>
            <input v-model.trim="form.username" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入用户名" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-700">密码</label>
            <input v-model="form.password" type="password" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入密码" />
          </div>
          <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>
          <AppButton block :disabled="loading">{{ loading ? '登录中...' : '登录' }}</AppButton>
        </form>

        <p class="mt-4 text-sm text-soft">还没有账号？<RouterLink to="/register" class="text-slate-900 underline">去注册</RouterLink></p>
      </AppCard>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import { useUserStore } from '@/stores/useUserStore'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

const submit = async () => {
  if (!form.username || !form.password) {
    error.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await userStore.login(form)
    const redirect = route.query.redirect ? String(route.query.redirect) : '/'
    router.push(redirect)
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
