import request from './request'

export const authApi = {
  login(data) {
    return request({ url: '/auth/login', method: 'post', data })
  },
  register(data) {
    return request({ url: '/auth/register', method: 'post', data })
  },
  sendVerificationCode(data) {
    return request({ url: '/auth/verification-code', method: 'post', data })
  },
  logout() {
    return request({ url: '/auth/logout', method: 'post' })
  }
}

export const aiApi = {
  chat(data) {
    return request({ url: '/ai/chat', method: 'post', data })
  }
}

export const userApi = {
  getUserInfo() {
    return request({ url: '/user/info', method: 'get' })
  },
  getPublicProfile(userId) {
    return request({ url: `/user/public/${userId}`, method: 'get' })
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
  },
  createProblem(data) {
    return request({ url: '/problem', method: 'post', data })
  },
  updateProblem(id, data) {
    return request({ url: `/problem/${id}`, method: 'put', data })
  },
  deleteProblem(id) {
    return request({ url: `/problem/${id}`, method: 'delete' })
  }
}

export const testCaseApi = {
  getProblemTestCases(problemId) {
    return request({ url: `/problem/${problemId}/test-cases`, method: 'get' })
  },
  replaceProblemTestCases(problemId, data) {
    return request({ url: `/problem/${problemId}/test-cases`, method: 'put', data })
  }
}

export const submissionApi = {
  submitCode(data) {
    return request({ url: '/submission/submit', method: 'post', data })
  },
  getSubmissionById(id) {
    return request({ url: `/submission/${id}`, method: 'get' })
  },
  getSubmissionStatus(id, config = {}) {
    return request({ url: `/submission/${id}/status`, method: 'get', ...config })
  },
  getSubmissionList(params) {
    return request({ url: '/submission/list', method: 'get', params })
  },
  getPointRanking(params) {
    return request({ url: '/submission/points/ranking', method: 'get', params })
  },
  getMyPointSummary() {
    return request({ url: '/submission/points/me', method: 'get' })
  }
}

export const contestApi = {
  getContestList(params) {
    return request({ url: '/contest/list', method: 'get', params })
  },
  getContestDetail(id) {
    return request({ url: `/contest/${id}`, method: 'get' })
  },
  createContest(data) {
    return request({ url: '/contest', method: 'post', data })
  },
  updateContest(id, data) {
    return request({ url: `/contest/${id}`, method: 'put', data })
  },
  deleteContest(id) {
    return request({ url: `/contest/${id}`, method: 'delete' })
  },
  joinContest(id) {
    return request({ url: `/contest/${id}/join`, method: 'post' })
  },
  getContestRanking(id, params) {
    return request({ url: `/contest/${id}/ranking`, method: 'get', params })
  },
  exportContestRanking(id) {
    return request({ url: `/contest/${id}/ranking/export`, method: 'get' })
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
  auditPost(id, data) {
    return request({ url: `/discussion/${id}/audit`, method: 'put', data })
  },
  likePost(id) {
    return request({ url: `/discussion/${id}/like`, method: 'post' })
  },
  unlikePost(id) {
    return request({ url: `/discussion/${id}/like`, method: 'delete' })
  },
  getPostDetail(id) {
    return request({ url: `/discussion/${id}`, method: 'get' })
  },
  getCommentList(postId, params) {
    return request({ url: `/discussion/${postId}/comments`, method: 'get', params })
  },
  createComment(postId, data) {
    return request({ url: `/discussion/${postId}/comments`, method: 'post', data })
  },
  deleteComment(commentId) {
    return request({ url: `/discussion/comments/${commentId}`, method: 'delete' })
  }
}

export const socialApi = {
  getFollowStatus(targetUserId) {
    return request({ url: '/social/follow/status', method: 'get', params: { targetUserId } })
  },
  follow(targetUserId) {
    return request({ url: `/social/follow/${targetUserId}`, method: 'post' })
  },
  unfollow(targetUserId) {
    return request({ url: `/social/follow/${targetUserId}`, method: 'delete' })
  },
  getFollowing(params) {
    return request({ url: '/social/follow/following', method: 'get', params })
  },
  getFollowers(params) {
    return request({ url: '/social/follow/followers', method: 'get', params })
  },
  sendMessage(data) {
    return request({ url: '/social/message', method: 'post', data })
  },
  getMessageList(params) {
    return request({ url: '/social/message/list', method: 'get', params })
  },
  getMessageThreads(params) {
    return request({ url: '/social/message/threads', method: 'get', params })
  },
  markConversationRead(peerUserId) {
    return request({ url: '/social/message/read', method: 'put', params: { peerUserId } })
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
