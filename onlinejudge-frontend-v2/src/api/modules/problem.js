import request from '../request'

export function fetchProblemHub(params = {}) {
  return request({
    url: '/problem/list',
    method: 'get',
    params
  })
}

export function fetchProblemDetail(id) {
  return request({
    url: `/problem/${id}`,
    method: 'get'
  })
}
