import request from '../request'

export function fetchDiscussionFeed(params = {}) {
  return request({
    url: '/discussion/list',
    method: 'get',
    params
  })
}
