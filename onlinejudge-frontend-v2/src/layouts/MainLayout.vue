<template>
  <div class="min-h-screen px-4 pb-8 pt-4 md:px-8">
    <header class="glass-panel sticky top-4 z-30 mx-auto mb-6 flex w-full max-w-[1400px] items-center justify-between rounded-xl border border-white/60 px-5 py-3 shadow-card">
      <div class="flex items-center gap-3">
        <div class="h-9 w-9 animate-floaty rounded-lg bg-gradient-to-br from-sky-500 to-blue-600" />
        <div>
          <div class="text-sm font-semibold text-ink">{{ app.siteName }}</div>
          <div class="text-xs text-soft">{{ app.announcement }}</div>
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
          <RouterLink to="/profile" class="flex h-10 w-10 items-center justify-center rounded-full border border-line bg-white text-sm font-semibold text-slate-700 transition hover:border-slate-800">
            {{ shortName }}
          </RouterLink>
          <AppButton size="sm" variant="ghost" @click="logout">退出</AppButton>
        </template>
      </div>
    </header>

    <main class="mx-auto max-w-[1400px]">
      <RouterView v-slot="{ Component }">
        <Transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import { useAppStore } from '@/stores/useAppStore'
import { useUserStore } from '@/stores/useUserStore'

const app = useAppStore()
const user = useUserStore()
const route = useRoute()
const router = useRouter()

const navItems = computed(() => {
  const common = [
    { to: '/', label: '首页指挥舱' },
    { to: '/problems', label: '题库中心' },
    { to: '/contests', label: '赛事中枢' },
    { to: '/discuss', label: '交流广场' },
    { to: '/studio', label: '代码工坊' }
  ]
  if (user.isTeacher || user.isAdmin) {
    common.push({ to: '/teacher-workbench', label: '教师工作台' })
  }
  if (user.isAdmin) {
    common.push({ to: '/admin-console', label: '管理控制台' })
  }
  return common
})

const shortName = computed(() => {
  const nick = user.userInfo?.nickname || user.userInfo?.username || '访客'
  return nick.slice(0, 1)
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
