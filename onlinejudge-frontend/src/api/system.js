import request from './request'

export const systemApi = {
  getPublicConfigs(params) {
    return request({
      url: '/system/public-configs',
      method: 'get',
      params
    })
  },

  getConfigs() {
    return request({
      url: '/admin/system/configs',
      method: 'get'
    })
  },

  upsertConfig(data) {
    return request({
      url: '/admin/system/config',
      method: 'put',
      data
    })
  },

  getLogs(params) {
    return request({
      url: '/admin/system/logs',
      method: 'get',
      params
    })
  },

  getJudgeResults(params) {
    return request({
      url: '/admin/system/judge-results',
      method: 'get',
      params
    })
  },

  getFeatureChecklist() {
    return request({
      url: '/admin/system/feature-checklist',
      method: 'get'
    })
  },

  getMonitor() {
    return request({
      url: '/admin/system/monitor',
      method: 'get'
    })
  }
}
