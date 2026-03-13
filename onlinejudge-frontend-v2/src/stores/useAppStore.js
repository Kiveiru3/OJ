import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    siteName: 'OJ Nova',
    announcement: '欢迎来到新一代在线评测前端演示版',
    user: {
      id: 0,
      nickname: '未登录',
      role: 'GUEST'
    }
  }),
  actions: {
    setUser(user) {
      this.user = { ...this.user, ...user }
    }
  }
})
