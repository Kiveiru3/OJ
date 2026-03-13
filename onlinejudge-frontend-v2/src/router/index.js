import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/useUserStore'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { requiresAuth: true } },
      { path: 'problems', name: 'problems', component: () => import('@/views/ProblemHubView.vue'), meta: { requiresAuth: true } },
      { path: 'contests', name: 'contests', component: () => import('@/views/ContestHubView.vue'), meta: { requiresAuth: true } },
      { path: 'discuss', name: 'discuss', component: () => import('@/views/DiscussionPlazaView.vue'), meta: { requiresAuth: true } },
      { path: 'studio', name: 'studio', component: () => import('@/views/CodeStudioView.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const user = useUserStore()

  if (to.meta.public && user.isLoggedIn) {
    await user.ensureUserInfo().catch(() => null)
    return '/'
  }

  if (to.meta.requiresAuth && !user.isLoggedIn) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  if (to.meta.requiresAuth) {
    try {
      await user.ensureUserInfo()
    } catch (_) {
      user.clearSession()
      return `/login?redirect=${encodeURIComponent(to.fullPath)}`
    }
  }

  return true
})

export default router
