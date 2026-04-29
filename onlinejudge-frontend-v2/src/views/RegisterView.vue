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
            <input v-model.trim="form.email" type="email" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入邮箱" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-700">手机号</label>
            <input v-model.trim="form.phone" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入手机号" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-700">验证码</label>
            <div class="flex gap-2">
              <input v-model.trim="form.verificationCode" class="min-w-0 flex-1 rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="6 位验证码" />
              <AppButton type="button" variant="secondary" :disabled="codeLoading || countdown > 0" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s` : (codeLoading ? '发送中...' : '发送') }}
              </AppButton>
            </div>
            <p v-if="codeTip" class="mt-1 text-xs text-soft">{{ codeTip }}</p>
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
import { onUnmounted, reactive, ref } from 'vue'
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
  phone: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
const error = ref('')
const codeTip = ref('')

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const phonePattern = /^1[3-9]\d{9}$/

let countdownTimer = null

onUnmounted(() => {
  window.clearInterval(countdownTimer)
})

const startCountdown = () => {
  countdown.value = 60
  window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const sendCode = async () => {
  error.value = ''
  codeTip.value = ''
  if (!phonePattern.test(form.phone)) {
    error.value = '请输入正确的手机号'
    return
  }
  codeLoading.value = true
  try {
    const res = await userStore.sendVerificationCode(form.phone)
    const demoCode = res.data?.code ? `，演示验证码：${res.data.code}` : ''
    codeTip.value = `验证码已发送${demoCode}`
    startCountdown()
  } catch (e) {
    error.value = e.message || '验证码发送失败'
  } finally {
    codeLoading.value = false
  }
}

const submit = async () => {
  error.value = ''
  if (!form.username || !form.email || !form.phone || !form.verificationCode || !form.password) {
    error.value = '请填写完整信息'
    return
  }
  if (!emailPattern.test(form.email)) {
    error.value = '请输入正确的邮箱'
    return
  }
  if (!phonePattern.test(form.phone)) {
    error.value = '请输入正确的手机号'
    return
  }
  if (!/^\d{6}$/.test(form.verificationCode)) {
    error.value = '请输入 6 位验证码'
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
      phone: form.phone,
      verificationCode: form.verificationCode,
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
