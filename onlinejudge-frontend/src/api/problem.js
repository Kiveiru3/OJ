import request from './request'

export const problemApi = {
  // 获取题目列表
  getProblemList(params) {
    return request({
      url: '/problem/list',
      method: 'get',
      params
    })
  },
  // 获取题目详情
  getProblemDetail(id) {
    return request({
      url: `/problem/${id}`,
      method: 'get'
    })
  },
  // 创建题目（教师/管理员）
  createProblem(data) {
    return request({
      url: '/problem/create',
      method: 'post',
      data
    })
  },
  // 更新题目（教师/管理员）
  updateProblem(id, data) {
    return request({
      url: `/problem/${id}`,
      method: 'put',
      data
    })
  },
  // 删除题目（管理员）
  deleteProblem(id) {
    return request({
      url: `/problem/${id}`,
      method: 'delete'
    })
  },
  // 获取题目的非样例测试用例（教师/管理员）
  getProblemTestCases(problemId) {
    return request({
      url: `/problem/${problemId}/test-cases`,
      method: 'get'
    })
  },
  // 新增测试用例（教师/管理员）
  createTestCase(problemId, data) {
    return request({
      url: `/problem/${problemId}/test-cases`,
      method: 'post',
      data
    })
  },
  // 更新测试用例（教师/管理员）
  updateTestCase(problemId, testCaseId, data) {
    return request({
      url: `/problem/${problemId}/test-cases/${testCaseId}`,
      method: 'put',
      data
    })
  },
  // 删除测试用例（教师/管理员）
  deleteTestCase(problemId, testCaseId) {
    return request({
      url: `/problem/${problemId}/test-cases/${testCaseId}`,
      method: 'delete'
    })
  }
}











