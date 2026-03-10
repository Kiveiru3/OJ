import request from './request'

export const submissionApi = {
  submitCode(data) {
    return request({
      url: '/submission/submit',
      method: 'post',
      data
    })
  },

  getSubmissionList(params) {
    return request({
      url: '/submission/list',
      method: 'get',
      params
    })
  },

  getSubmissionDetail(id) {
    return request({
      url: `/submission/${id}`,
      method: 'get'
    })
  },

  getSubmissionStatus(id, config = {}) {
    return request({
      url: `/submission/${id}/status`,
      method: 'get',
      ...config
    })
  },

  rejudgeSubmission(id) {
    return request({
      url: `/submission/${id}/rejudge`,
      method: 'post'
    })
  }
}

