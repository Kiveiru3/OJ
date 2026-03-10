import request from './request'

export const userApi = {
  getUserInfo() {
    return request({
      url: '/user/info',
      method: 'get'
    })
  },

  updateUserInfo(data) {
    return request({
      url: '/user/update',
      method: 'put',
      data
    })
  },

  changePassword(data) {
    return request({
      url: '/user/change-password',
      method: 'post',
      data
    })
  },

  getRoleProfile() {
    return request({
      url: '/user/role-profile',
      method: 'get'
    })
  },

  updateRoleProfile(data) {
    return request({
      url: '/user/role-profile',
      method: 'put',
      data
    })
  },

  getUserList(params) {
    return request({
      url: '/user/list',
      method: 'get',
      params
    })
  },

  adminUpdateUser(id, data) {
    return request({
      url: `/user/${id}/admin`,
      method: 'put',
      data
    })
  },

  adminResetPassword(id, data) {
    return request({
      url: `/user/${id}/reset-password`,
      method: 'post',
      data
    })
  },

  adminGetRoleProfile(id) {
    return request({
      url: `/user/${id}/role-profile`,
      method: 'get'
    })
  },

  adminUpdateRoleProfile(id, data) {
    return request({
      url: `/user/${id}/role-profile`,
      method: 'put',
      data
    })
  }
}
