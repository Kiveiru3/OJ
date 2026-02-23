import request from './request'

export const discussionApi = {
  getPostList(params) {
    return request({
      url: '/discussion/list',
      method: 'get',
      params
    })
  },

  getPostDetail(id) {
    return request({
      url: `/discussion/${id}`,
      method: 'get'
    })
  },

  createPost(data) {
    return request({
      url: '/discussion',
      method: 'post',
      data
    })
  },

  deletePost(id) {
    return request({
      url: `/discussion/${id}`,
      method: 'delete'
    })
  },

  getCommentList(postId, params) {
    return request({
      url: `/discussion/${postId}/comments`,
      method: 'get',
      params
    })
  },

  createComment(postId, data) {
    return request({
      url: `/discussion/${postId}/comments`,
      method: 'post',
      data
    })
  },

  deleteComment(commentId) {
    return request({
      url: `/discussion/comments/${commentId}`,
      method: 'delete'
    })
  }
}
