<template>
  <div class="flex min-h-screen flex-col px-4 pb-6 pt-4 md:px-8">
    <header class="glass-panel sticky top-4 z-30 mx-auto mb-6 flex w-full max-w-[1400px] items-center justify-between rounded-xl border border-line bg-white px-5 py-3 shadow-card">
      <div class="flex items-center gap-3">
        <div class="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 bg-slate-900 text-sm font-semibold text-white">OJ</div>
        <div>
          <div class="text-sm font-semibold text-ink">{{ app.siteName || '在线评测平台' }}</div>
        </div>
      </div>

      <nav class="hidden items-center gap-2 md:flex">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="rounded-lg px-3 py-2 text-sm transition"
          :class="isNavActive(item.to) ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-900 hover:text-white'"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="flex items-center gap-2">
        <RouterLink v-if="!user.isLoggedIn" to="/login">
          <AppButton size="sm" variant="secondary">登录</AppButton>
        </RouterLink>
        <RouterLink v-if="!user.isLoggedIn" to="/register">
          <AppButton size="sm">注册</AppButton>
        </RouterLink>

        <template v-else>
          <RouterLink :to="myHomePath" class="flex items-center gap-2 rounded-full border border-line bg-white px-2 py-1 text-sm transition hover:border-slate-800">
            <UserAvatar :user="user.userInfo || {}" size="sm" />
            <span class="max-w-[120px] truncate text-slate-700">{{ displayName }}</span>
          </RouterLink>
          <AppButton size="sm" variant="ghost" @click="logout">退出</AppButton>
        </template>
      </div>
    </header>

    <main class="mx-auto w-full max-w-[1400px] flex-1">
      <RouterView v-slot="{ Component }">
        <Transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <footer class="mx-auto mt-8 w-full max-w-[1400px] rounded-xl border border-line bg-white px-5 py-4 text-xs text-soft">
      <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <span>{{ app.siteName || '在线评测平台' }} · 程序设计教学系统</span>
        <span>支持：题库练习 / 在线判题 / 竞赛组织 / 讨论交流</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useAppStore } from '@/stores/useAppStore'
import { useUserStore } from '@/stores/useUserStore'
import { getDisplayName } from '@/utils/avatar'

const app = useAppStore()
const user = useUserStore()
const route = useRoute()
const router = useRouter()

const navItems = computed(() => {
  const common = [
    { to: '/', label: '首页' },
    { to: '/problems', label: '题库' },
    { to: '/contests', label: '竞赛' },
    { to: '/discuss', label: '讨论' }
  ]
  if (user.isLoggedIn) {
    common.push({ to: '/studio', label: '做题' })
    common.push({ to: '/ai-chat', label: 'AI助手' })
    common.push({ to: '/messages', label: '私信' })
  }
  if (user.isTeacher || user.isAdmin) {
    common.push({ to: '/teacher-workbench', label: '教师工作台' })
  }
  if (user.isAdmin) {
    common.push({ to: '/admin-console', label: '管理控制台' })
  }
  return common
})

const displayName = computed(() => getDisplayName(user.userInfo || {}))
const myHomePath = computed(() => {
  const id = Number(user.userInfo?.id || 0)
  return id > 0 ? `/users/${id}` : '/profile'
})

const logout = async () => {
  await user.logout()
  router.push('/login')
}

const isNavActive = (path) => {
  const currentPath = route.path
  if (path === '/') {
    return currentPath === '/'
  }
  return currentPath === path || currentPath.startsWith(`${path}/`)
}

onMounted(() => {
  app.loadPublicConfigs()
  if (user.isLoggedIn) {
    user.ensureUserInfo().catch(() => null)
  }
})
</script>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
