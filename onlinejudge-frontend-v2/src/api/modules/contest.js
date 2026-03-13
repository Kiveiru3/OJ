import request from '../request'

export function fetchContestHub(params = {}) {
  return request({
    url: '/contest/list',
    method: 'get',
    params
  })
}

export function fetchContestDetail(id) {
  return request({
    url: `/contest/${id}`,
    method: 'get'
  })
}
