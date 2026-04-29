import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    confirmVisible: false,
    confirmTitle: '',
    confirmMessage: '',
    confirmOkText: '确认',
    confirmCancelText: '取消',
    confirmShowCancel: true,
    _confirmResolver: null
  }),

  actions: {
    async confirm(options = {}) {
      if (this._confirmResolver) {
        this._confirmResolver(false)
        this._confirmResolver = null
      }

      this.confirmTitle = options.title || '提示'
      this.confirmMessage = options.message || ''
      this.confirmOkText = options.okText || '确认'
      this.confirmCancelText = options.cancelText || '取消'
      this.confirmShowCancel = options.showCancel !== false
      this.confirmVisible = true

      return new Promise((resolve) => {
        this._confirmResolver = resolve
      })
    },

    async alert(options = {}) {
      return this.confirm({
        title: options.title || '提示',
        message: options.message || '',
        okText: options.okText || '我知道了',
        showCancel: false
      })
    },

    resolveConfirm(result) {
      const resolver = this._confirmResolver
      this._confirmResolver = null
      this.confirmVisible = false
      if (resolver) {
        resolver(!!result)
      }
    }
  }
})
