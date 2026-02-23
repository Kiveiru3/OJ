import request from './request'

export const teacherApi = {
  getOverviewAnalytics(params) {
    return request({
      url: '/teacher/analytics/overview',
      method: 'get',
      params
    })
  },

  exportOverviewCsv(params) {
    return request({
      url: '/teacher/analytics/overview/export',
      method: 'get',
      params
    })
  }
}
