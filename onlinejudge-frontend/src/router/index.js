import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    redirect: '/problems'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/problems',
    name: 'ProblemList',
    component: () => import('@/views/ProblemList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/problem/:id',
    name: 'ProblemDetail',
    component: () => import('@/views/ProblemDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/submissions',
    name: 'SubmissionList',
    component: () => import('@/views/SubmissionList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/contests',
    name: 'ContestList',
    component: () => import('@/views/ContestList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/contest/:id',
    name: 'ContestDetail',
    component: () => import('@/views/ContestDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/discussions',
    name: 'DiscussionList',
    component: () => import('@/views/DiscussionList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/discussion/:id',
    name: 'DiscussionDetail',
    component: () => import('@/views/DiscussionDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/submission/:id',
    name: 'SubmissionDetail',
    component: () => import('@/views/SubmissionDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/problems',
    name: 'AdminProblemList',
    component: () => import('@/views/admin/ProblemManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUserList',
    component: () => import('@/views/admin/UserManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/system',
    name: 'AdminSystem',
    component: () => import('@/views/admin/SystemManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/teacher/problems',
    name: 'TeacherProblemList',
    component: () => import('@/views/teacher/ProblemManage.vue'),
    meta: { requiresAuth: true, requiresTeacher: true }
  },
  {
    path: '/teacher/analytics',
    name: 'TeacherAnalytics',
    component: () => import('@/views/teacher/Analytics.vue'),
    meta: { requiresAuth: true, requiresTeacher: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 已登录用户访问登录/注册页时直接回到题库
  if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
    try {
      await userStore.ensureUserInfo()
    } catch (error) {
      await userStore.logout()
      return true
    }
    return { name: 'ProblemList' }
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAuth) {
    try {
      await userStore.ensureUserInfo()
    } catch (error) {
      await userStore.logout()
      return { name: 'Login', query: { redirect: to.fullPath } }
    }

    if (to.meta.requiresAdmin && !userStore.isAdmin) {
      return { name: 'ProblemList' }
    }

    // 管理页面允许教师和管理员访问
    if (to.meta.requiresTeacher && !userStore.isTeacher && !userStore.isAdmin) {
      return { name: 'ProblemList' }
    }
  }

  return true
})

export default router
