import request from './request'

export const contestApi = {
  getContestList(params) {
    return request({
      url: '/contest/list',
      method: 'get',
      params
    })
  },

  getContestDetail(id) {
    return request({
      url: `/contest/${id}`,
      method: 'get'
    })
  },

  createContest(data) {
    return request({
      url: '/contest',
      method: 'post',
      data
    })
  },

  updateContest(id, data) {
    return request({
      url: `/contest/${id}`,
      method: 'put',
      data
    })
  },

  deleteContest(id) {
    return request({
      url: `/contest/${id}`,
      method: 'delete'
    })
  },

  joinContest(id) {
    return request({
      url: `/contest/${id}/join`,
      method: 'post'
    })
  },

  getContestRanking(id, params) {
    return request({
      url: `/contest/${id}/ranking`,
      method: 'get',
      params
    })
  },

  getContestScoreSnapshot(id, params) {
    return request({
      url: `/contest/${id}/score-snapshot`,
      method: 'get',
      params
    })
  },

  getContestAnalytics(id) {
    return request({
      url: `/contest/${id}/analytics`,
      method: 'get'
    })
  },

  exportContestRanking(id) {
    return request({
      url: `/contest/${id}/ranking/export`,
      method: 'get'
    })
  }
}
