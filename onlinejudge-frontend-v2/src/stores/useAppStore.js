import { defineStore } from 'pinia'
import { systemApi } from '@/api'

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
    },
    async loadPublicConfigs() {
      try {
        const res = await systemApi.getPublicConfigs()
        const cfg = res?.data || {}
        if (cfg['site.name']) this.siteName = cfg['site.name']
        if (cfg['site.announcement'] !== undefined) {
          this.announcement = cfg['site.announcement'] || '欢迎来到在线评测平台'
        }
      } catch (_) {
        // keep local defaults when backend config is unavailable
      }
    }
  }
})
