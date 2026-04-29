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
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
      { path: 'problems', name: 'problems', component: () => import('@/views/ProblemHubView.vue') },
      { path: 'contests', name: 'contests', component: () => import('@/views/ContestHubView.vue') },
      { path: 'discuss', name: 'discuss', component: () => import('@/views/DiscussionPlazaView.vue') },
      { path: 'discuss/:id', name: 'discussDetail', component: () => import('@/views/DiscussionDetailView.vue') },
      { path: 'studio', name: 'studio', component: () => import('@/views/CodeStudioView.vue'), meta: { requiresAuth: true } },
      { path: 'messages', name: 'messages', component: () => import('@/views/MessageCenterView.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true } },
      { path: 'users/:id', name: 'userHome', component: () => import('@/views/UserHomeView.vue'), meta: { requiresAuth: true } },
      {
        path: 'teacher-workbench',
        name: 'teacherWorkbench',
        component: () => import('@/views/TeacherWorkbenchView.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER', 'ADMIN'] }
      },
      {
        path: 'admin-console',
        name: 'adminConsole',
        component: () => import('@/views/AdminConsoleView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      }
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

  const requiredRoles = to.meta?.roles
  if (requiredRoles?.length) {
    const role = user.userInfo?.role
    if (!requiredRoles.includes(role)) {
      return '/'
    }
  }

  return true
})

export default router
