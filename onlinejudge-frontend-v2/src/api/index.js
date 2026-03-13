import request from './request'

export const authApi = {
  login(data) {
    return request({ url: '/auth/login', method: 'post', data })
  },
  register(data) {
    return request({ url: '/auth/register', method: 'post', data })
  },
  logout() {
    return request({ url: '/auth/logout', method: 'post' })
  }
}

export const userApi = {
  getUserInfo() {
    return request({ url: '/user/info', method: 'get' })
  },
  updateUserInfo(data) {
    return request({ url: '/user/update', method: 'put', data })
  },
  changePassword(data) {
    return request({ url: '/user/change-password', method: 'post', data })
  },
  getRoleProfile() {
    return request({ url: '/user/role-profile', method: 'get' })
  },
  updateRoleProfile(data) {
    return request({ url: '/user/role-profile', method: 'put', data })
  },
  getUserList(params) {
    return request({ url: '/user/list', method: 'get', params })
  },
  adminUpdateUser(id, data) {
    return request({ url: `/user/${id}/admin`, method: 'put', data })
  },
  adminResetPassword(id, data) {
    return request({ url: `/user/${id}/reset-password`, method: 'post', data })
  },
  adminGetRoleProfile(id) {
    return request({ url: `/user/${id}/role-profile`, method: 'get' })
  },
  adminUpdateRoleProfile(id, data) {
    return request({ url: `/user/${id}/role-profile`, method: 'put', data })
  }
}

export const problemApi = {
  getProblemList(params) {
    return request({ url: '/problem/list', method: 'get', params })
  },
  getProblemDetail(id) {
    return request({ url: `/problem/${id}`, method: 'get' })
  }
}

export const submissionApi = {
  submitCode(data) {
    return request({ url: '/submission/submit', method: 'post', data })
  },
  getSubmissionStatus(id, config = {}) {
    return request({ url: `/submission/${id}/status`, method: 'get', ...config })
  },
  getSubmissionList(params) {
    return request({ url: '/submission/list', method: 'get', params })
  }
}

export const contestApi = {
  getContestList(params) {
    return request({ url: '/contest/list', method: 'get', params })
  },
  getContestDetail(id) {
    return request({ url: `/contest/${id}`, method: 'get' })
  },
  joinContest(id) {
    return request({ url: `/contest/${id}/join`, method: 'post' })
  },
  getContestRanking(id, params) {
    return request({ url: `/contest/${id}/ranking`, method: 'get', params })
  }
}

export const discussionApi = {
  getPostList(params) {
    return request({ url: '/discussion/list', method: 'get', params })
  },
  createPost(data) {
    return request({ url: '/discussion', method: 'post', data })
  },
  deletePost(id) {
    return request({ url: `/discussion/${id}`, method: 'delete' })
  },
  getPostDetail(id) {
    return request({ url: `/discussion/${id}`, method: 'get' })
  }
}

export const teacherApi = {
  getOverview(params) {
    return request({ url: '/teacher/analytics/overview', method: 'get', params })
  },
  exportOverview(params) {
    return request({ url: '/teacher/analytics/overview/export', method: 'get', params })
  }
}

export const adminApi = {
  getConfigs() {
    return request({ url: '/admin/system/configs', method: 'get' })
  },
  upsertConfig(data) {
    return request({ url: '/admin/system/config', method: 'put', data })
  },
  getLogs(params) {
    return request({ url: '/admin/system/logs', method: 'get', params })
  },
  getMonitor() {
    return request({ url: '/admin/system/monitor', method: 'get' })
  },
  getJudgeResults(params) {
    return request({ url: '/admin/system/judge-results', method: 'get', params })
  }
}

export const systemApi = {
  getPublicConfigs(params) {
    return request({ url: '/system/public-configs', method: 'get', params })
  }
}
