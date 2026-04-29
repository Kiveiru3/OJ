import { defineStore } from 'pinia'
import { systemApi } from '@/api'

export const useAppStore = defineStore('app', {
  state: () => ({
    siteName: 'OJ Nova',
    announcement: '',
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
          this.announcement = cfg['site.announcement'] || ''
        }
      } catch (_) {
        // keep local defaults when backend config is unavailable
      }
    }
  }
})
