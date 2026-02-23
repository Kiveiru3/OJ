import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    userInfoPromise: null
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 'ADMIN',
    isTeacher: (state) => state.userInfo?.role === 'TEACHER',
    isStudent: (state) => state.userInfo?.role === 'STUDENT'
  },
  
  actions: {
    clearSession() {
      this.token = ''
      this.userInfo = null
      this.userInfoPromise = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },

    async login(loginForm) {
      const res = await authApi.login(loginForm)
      this.token = res.data.token
      this.userInfo = res.data.userInfo
      localStorage.setItem('token', this.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return res
    },
    
    async register(registerForm) {
      return await authApi.register(registerForm)
    },
    
    async logout() {
      try {
        await authApi.logout()
      } catch (error) {
        // Ignore logout network errors and still clear local session.
      } finally {
        this.clearSession()
      }
    },
    
    async fetchUserInfo() {
      const res = await userApi.getUserInfo()
      this.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return res
    },

    async ensureUserInfo() {
      if (!this.token) {
        return null
      }
      if (this.userInfo) {
        return this.userInfo
      }
      if (this.userInfoPromise) {
        return this.userInfoPromise
      }

      this.userInfoPromise = this.fetchUserInfo()
        .then((res) => res.data)
        .finally(() => {
          this.userInfoPromise = null
        })
      return this.userInfoPromise
    }
  }
})
