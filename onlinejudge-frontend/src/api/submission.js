import request from './request'

export const submissionApi = {
  // 提交代码
  submitCode(data) {
    return request({
      url: '/submission/submit',
      method: 'post',
      data
    })
  },
  // 获取提交记录
  getSubmissionList(params) {
    return request({
      url: '/submission/list',
      method: 'get',
      params
    })
  },
  // 获取提交详情
  getSubmissionDetail(id) {
    return request({
      url: `/submission/${id}`,
      method: 'get'
    })
  },
  // 获取提交评测状态
  getSubmissionStatus(id, config = {}) {
    return request({
      url: `/submission/${id}/status`,
      method: 'get',
      ...config
    })
  }
}











