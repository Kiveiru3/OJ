import { defineStore } from 'pinia'
import { systemApi } from '@/api'

const DEFAULT_CONFIG = {
  'site.name': 'Online Judge',
  'site.announcement': '',
  'contest.default_page_size': '10',
  'contest.default_penalty_per_wrong': '20'
}

export const useSystemStore = defineStore('system', {
  state: () => ({
    configMap: { ...DEFAULT_CONFIG },
    loaded: false,
    loadingPromise: null
  }),

  getters: {
    siteName: (state) => {
      const name = state.configMap['site.name']
      return typeof name === 'string' && name.trim() ? name.trim() : DEFAULT_CONFIG['site.name']
    },
    siteAnnouncement: (state) => {
      const value = state.configMap['site.announcement']
      return typeof value === 'string' ? value.trim() : ''
    },
    contestDefaultPageSize: (state) => {
      const value = Number(state.configMap['contest.default_page_size'])
      if (!Number.isFinite(value)) {
        return Number(DEFAULT_CONFIG['contest.default_page_size'])
      }
      const rounded = Math.floor(value)
      if (rounded < 1) return 10
      if (rounded > 100) return 100
      return rounded
    },
    contestDefaultPenaltyPerWrong: (state) => {
      const value = Number(state.configMap['contest.default_penalty_per_wrong'])
      if (!Number.isFinite(value)) {
        return Number(DEFAULT_CONFIG['contest.default_penalty_per_wrong'])
      }
      const rounded = Math.floor(value)
      if (rounded < 0) return 0
      if (rounded > 120) return 120
      return rounded
    }
  },

  actions: {
    reset() {
      this.configMap = { ...DEFAULT_CONFIG }
      this.loaded = false
      this.loadingPromise = null
    },

    mergeConfigMap(data) {
      if (!data || typeof data !== 'object') {
        return
      }
      this.configMap = {
        ...DEFAULT_CONFIG,
        ...this.configMap,
        ...data
      }
    },

    async fetchPublicConfigs(force = false) {
      if (this.loaded && !force) {
        return this.configMap
      }
      if (this.loadingPromise && !force) {
        return this.loadingPromise
      }

      this.loadingPromise = systemApi.getPublicConfigs().then((res) => {
        this.mergeConfigMap(res.data || {})
        this.loaded = true
        return this.configMap
      }).finally(() => {
        this.loadingPromise = null
      })

      return this.loadingPromise
    },

    async ensureLoaded() {
      if (this.loaded) {
        return this.configMap
      }
      return this.fetchPublicConfigs()
    }
  }
})
