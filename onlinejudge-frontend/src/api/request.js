import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json'
  }
})

let isRedirectingToLogin = false

const redirectToLogin = async () => {
  if (isRedirectingToLogin) {
    return
  }
  isRedirectingToLogin = true

  try {
    const userStore = useUserStore()
    userStore.clearSession()

    if (router.currentRoute.value.path !== '/login') {
      await router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath }
      })
    }
  } finally {
    isRedirectingToLogin = false
  }
}

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  async (response) => {
    const res = response.data
    const silent = Boolean(response.config?.silent)

    if (!res || typeof res !== 'object') {
      const message = 'Invalid response format'
      if (!silent) {
        ElMessage.error(message)
      }
      return Promise.reject(new Error(message))
    }

    if (res.code !== 200) {
      let message = res.message || 'Request failed'
      if (!silent) {
        ElMessage.error(message)
      }

      if (res.code === 401) {
        await redirectToLogin()
      } else if (res.code === 429) {
        // keep server message, fallback to a clear throttling prompt
        if (!res.message || res.message === 'Too Many Requests') {
          message = '操作过于频繁，请稍后再试'
        }
      }

      return Promise.reject(new Error(message))
    }

    return res
  },
  async (error) => {
    let message = error.message || 'Network error'
    const silent = Boolean(error.config?.silent)

    if (error.response) {
      const { status, data } = error.response
      message = data?.message || message

      if (status === 401) {
        message = data?.message || 'Unauthorized, please login again'
        await redirectToLogin()
      } else if (status === 403) {
        message = data?.message || 'Forbidden'
      } else if (status === 429) {
        message = data?.message || '操作过于频繁，请稍后再试'
      } else if (status === 404) {
        message = data?.message || 'Resource not found'
      } else if (status >= 500) {
        message = data?.message || 'Server error'
      }
    } else if (error.request) {
      message = 'Network connection failed'
    }

    if (!silent) {
      ElMessage.error(message)
    }
    return Promise.reject(new Error(message))
  }
)

export default request
